package com.maf.exchangeapiv1.service;

import com.maf.exchangeapiv1.dto.ConversionDto;
import com.maf.exchangeapiv1.dto.FinalPriceDto;
import com.maf.exchangeapiv1.dto.converter.ConversionConverter;
import com.maf.exchangeapiv1.model.Account;
import com.maf.exchangeapiv1.model.Conversion;
import com.maf.exchangeapiv1.model.ConversionStatus;
import com.maf.exchangeapiv1.model.Transaction;
import com.maf.exchangeapiv1.repository.ConversionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversionService {

    private static final String COMPANY_USER_ID = "company-user-id";
    private static final String COINBASE_USER_ID = "coinbase-system-id";

    private final ConversionRepository conversionRepository;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final SpreadEngineService spreadEngineService;
    private final ConversionConverter conversionConverter;

    public ConversionDto getConversionById(String conversionId, String userId) {
        Conversion conversion = conversionRepository.findById(conversionId)
                .orElseThrow(() -> new RuntimeException("Conversion not found: " + conversionId));
        if (!conversion.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to conversion: " + conversionId);
        }
        return conversionConverter.toDTO(conversion);
    }

    // ==================== OFFER OPERATIONS ====================

    @Transactional
    public ConversionDto createConversionOffer(String userId, String fromAsset, String toAsset, BigDecimal amount, String description, String side, String conversionType, String idempotencyKey) {

        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            throw new RuntimeException("Idempotency key is required!");
        }

        Optional<Conversion> existing = conversionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("[IDEMPOTENCY] Duplicate request for key: {}", idempotencyKey);
            return conversionConverter.toDTO(existing.get());
        }

        String pair = toAsset + "-" + fromAsset;
        FinalPriceDto price;
        if ("BASE".equalsIgnoreCase(conversionType)) {
            price = spreadEngineService.calculateBaseFinalPrice(amount, pair, side);
        } else {
            price = spreadEngineService.calculateQuoteFinalPrice(amount, pair, side);
        }

        if (price == null || price.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Could not fetch live rate for " + pair);
        }
        Account fromAccount = accountService.getAccountById(userId, fromAsset);
        Account toAccount = accountService.getAccountById(userId, toAsset);
        BigDecimal exchangeFee = price.getExchangeFee();
        BigDecimal companyFee = price.getCompanyFee();
        BigDecimal slippageFee = price.getSlippageAmount();
        BigDecimal totalFees = price.getTotalFees();
        Conversion conversion = Conversion.builder()
                .idempotencyKey(idempotencyKey)
                .userId(userId)
                .fromAsset(fromAsset)
                .toAsset(toAsset)
                .fromAmount(price.getTotalCost())
                .toAmount(price.getOutputAmount())
                .rate(price.getUnitPrice())
                .fromAccountId(fromAccount.getId())
                .toAccountId(toAccount.getId())
                .exchangeFee(exchangeFee)
                .companyFee(companyFee)
                .slippageFee(slippageFee)
                .totalFees(totalFees)
                .status(ConversionStatus.PENDING)
                .description(description != null ? description : "Conversion offer")
                .build();
        Conversion savedConversion = conversionRepository.save(conversion);
        log.info("[CONVERSION] Offer created: {} {} -> {} {} (Rate: {}, Fees: {})", amount, fromAsset, price.getOutputAmount(), toAsset, price.getUnitPrice(), totalFees);
        return conversionConverter.toDTO(savedConversion);
    }

    // ==================== ACCEPT / COMPLETE ====================

    @Transactional
    public ConversionDto acceptConversionOffer(String conversionId, String userId) {
        Conversion conversion = validateAndGetConversion(conversionId, userId);
        validateConversionStatus(conversion);
        validateConversionNotExpired(conversion);

        return completeConversion(conversion);
    }

    @Transactional
    public ConversionDto completeConversion(Conversion conversion) {
        BigDecimal fromAccountBalanceBefore = accountService.getBalance(conversion.getUserId(), conversion.getFromAsset());
        BigDecimal toAccountBalanceBefore = accountService.getBalance(conversion.getUserId(), conversion.getToAsset());

        Account fromAccount = accountService.debitAccount(
                conversion.getUserId(),
                conversion.getFromAsset(),
                conversion.getFromAmount()
        );

        Account toAccount = accountService.creditAccount(
                conversion.getUserId(),
                conversion.getToAsset(),
                conversion.getToAmount()
        );

        // Company Exchange Process
        BigDecimal companyFee = conversion.getCompanyFee();
        if (companyFee != null && companyFee.compareTo(BigDecimal.ZERO) > 0) {
            Account companyAccount = accountService.creditCompanyAccount(
                    conversion.getFromAsset(),
                    companyFee,
                    conversion.getId()
            );
            Transaction companyTx = transactionService.createTransaction(
                    COMPANY_USER_ID,
                    companyAccount.getId(),
                    conversion.getFromAsset(),
                    "COMPANY_FEE_IN",
                    companyFee,
                    conversion.getRate(),
                    conversion.getId(),
                    "Company fee from conversion: " + conversion.getId(),
                    companyAccount.getBalance().subtract(companyFee),
                    companyAccount.getBalance()
                    ,null
            );
            log.info("[CONVERSION] Company fee credited: {} {} (Ref: {}, Tx: {})", companyFee, conversion.getFromAsset(), conversion.getId(), companyTx.getId());
        }

        //Coinbase Exchange Process
        BigDecimal exchangeFee = conversion.getExchangeFee();
        if (exchangeFee != null && exchangeFee.compareTo(BigDecimal.ZERO) > 0) {
            Account coinbaseAccount = accountService.creditCoinbaseAccount(
                    conversion.getFromAsset(),
                    exchangeFee,
                    conversion.getId()
            );
            Transaction coinbaseTx = transactionService.createTransaction(
                    COINBASE_USER_ID,
                    coinbaseAccount.getId(),
                    conversion.getFromAsset(),
                    "EXCHANGE_FEE_IN",
                    exchangeFee,
                    conversion.getRate(),
                    conversion.getId(),
                    "Exchange fee from conversion: " + conversion.getId(),
                    coinbaseAccount.getBalance().subtract(exchangeFee),
                    coinbaseAccount.getBalance(),
                    null
            );
            log.info("[CONVERSION] Exchange fee credited: {} {} (Ref: {}, Tx: {})", exchangeFee, conversion.getFromAsset(), conversion.getId(), coinbaseTx.getId());
        }

        //User Transactions
        Transaction fromTx = transactionService.createTransaction(
                conversion.getUserId(),
                fromAccount.getId(),
                conversion.getFromAsset(),
                "CONVERSION_OUT",
                conversion.getFromAmount(),
                conversion.getRate(),
                conversion.getId(),
                "Conversion out: " + conversion.getFromAmount() + " " + conversion.getFromAsset(),
                fromAccountBalanceBefore,
                fromAccount.getBalance(),
                conversion.getTotalFees()
        );
        Transaction toTx = transactionService.createTransaction(
                conversion.getUserId(),
                toAccount.getId(),
                conversion.getToAsset(),
                "CONVERSION_IN",
                conversion.getToAmount(),
                conversion.getRate(),
                conversion.getId(),
                "Conversion in: " + conversion.getToAmount() + " " + conversion.getToAsset(),
                toAccountBalanceBefore,
                toAccount.getBalance(),
                conversion.getTotalFees()
        );
        conversion.setStatus(ConversionStatus.COMPLETED);
        conversion.setCompletedAt(LocalDateTime.now());
        conversion.setTransactionId(fromTx.getId());
        conversionRepository.save(conversion);

        log.info("[CONVERSION] Completed: {} {} -> {} {} (Company Fee: {}, Exchange Fee: {})",
                conversion.getFromAmount(), conversion.getFromAsset(),
                conversion.getToAmount(), conversion.getToAsset(),
                companyFee, exchangeFee);

        return conversionConverter.toDTO(conversion);
    }

    // ==================== CANCEL ====================

    @Transactional
    public ConversionDto cancelConversion(String conversionId, String userId) {
        Conversion conversion = validateAndGetConversion(conversionId, userId);
        validateConversionStatus(conversion);

        conversion.setStatus(ConversionStatus.CANCELLED);
        conversionRepository.save(conversion);

        log.info("[CONVERSION] Cancelled: {}", conversionId);
        return conversionConverter.toDTO(conversion);
    }

    // ==================== SCHEDULED TASKS ====================

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void expirePendingConversions() {
        List<Conversion> pendingConversions = conversionRepository.findByStatus(ConversionStatus.PENDING);
        LocalDateTime now = LocalDateTime.now();

        for (Conversion conversion : pendingConversions) {
            if (conversion.getExpiresAt() != null && conversion.getExpiresAt().isBefore(now)) {
                conversion.setStatus(ConversionStatus.EXPIRED);
                conversionRepository.save(conversion);
                log.info("[CONVERSION] Expired: {}", conversion.getId());
            }
        }
    }


    // ==================== VALIDATION ====================

    private Conversion validateAndGetConversion(String conversionId, String userId) {
        Conversion conversion = conversionRepository.findById(conversionId).orElseThrow(() -> new RuntimeException("Conversion not found: " + conversionId));

        if (!conversion.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to conversion: " + conversionId);
        }

        return conversion;
    }

    private void validateConversionStatus(Conversion conversion) {
        if (conversion.getStatus() != ConversionStatus.PENDING) {
            throw new RuntimeException("Conversion is already " + conversion.getStatus());
        }
    }

    private void validateConversionNotExpired(Conversion conversion) {
        if (conversion.getExpiresAt() != null && conversion.getExpiresAt().isBefore(LocalDateTime.now())) {
            conversion.setStatus(ConversionStatus.EXPIRED);
            conversionRepository.save(conversion);
            throw new RuntimeException("Conversion offer has expired: " + conversion.getId());
        }
    }
}
