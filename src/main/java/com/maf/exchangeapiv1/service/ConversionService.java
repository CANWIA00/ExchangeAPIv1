package com.maf.exchangeapiv1.service;

import com.maf.exchangeapiv1.dto.ConversionDto;
import com.maf.exchangeapiv1.dto.FinalPriceDto;
import com.maf.exchangeapiv1.dto.converter.ConversionConverter;
import com.maf.exchangeapiv1.model.Account;
import com.maf.exchangeapiv1.model.Conversion;
import com.maf.exchangeapiv1.model.ConversionStatus;
import com.maf.exchangeapiv1.model.Transaction;
import com.maf.exchangeapiv1.repository.AccountRepository;
import com.maf.exchangeapiv1.repository.ConversionRepository;
import com.maf.exchangeapiv1.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversionService {

    private static final long OFFER_TTL_SECONDS = 30;
    private static final BigDecimal COMPANY_FEE_RATE = new BigDecimal("0.002");

    private final ConversionRepository conversionRepository;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final SpreadEngineService spreadEngineService;
    private final ConversionConverter conversionConverter;

    // ==================== READ OPERATIONS ====================

    public List<ConversionDto> getUserConversions(String userId) {
        return conversionConverter.toDTOList(
                conversionRepository.findByUserIdOrderByCreatedAtDesc(userId)
        );
    }

    public ConversionDto getConversion(String conversionId) {
        Conversion conversion = conversionRepository.findById(conversionId)
                .orElseThrow(() -> new RuntimeException("Conversion not found!"));
        return conversionConverter.toDTO(conversion);
    }

    // ==================== OFFER OPERATIONS ====================

    @Transactional
    public ConversionDto createConversionOffer(String userId, String fromAsset, String toAsset,
                                               BigDecimal amount, String description,
                                               String side, String conversionType) {

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
                .userId(userId)
                .fromAsset(fromAsset)
                .toAsset(toAsset)
                .fromAmount(amount)
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

        log.info("[CONVERSION] Offer created: {} {} -> {} {} (Rate: {}, Fees: {})",
                amount, fromAsset, price.getOutputAmount(), toAsset, price.getUnitPrice(), totalFees);

        return conversionConverter.toDTO(savedConversion);
    }

    // ==================== ACCEPT / COMPLETE ====================

    @Transactional
    public ConversionDto acceptConversionOffer(String conversionId, String userId) {
        Conversion conversion = conversionRepository.findById(conversionId)
                .orElseThrow(() -> new RuntimeException("Conversion not found!"));

        if (!conversion.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access!");
        }

        if (conversion.getStatus() != ConversionStatus.PENDING) {
            throw new RuntimeException("Conversion is already " + conversion.getStatus());
        }

        if (conversion.getExpiresAt() != null && conversion.getExpiresAt().isBefore(LocalDateTime.now())) {
            conversion.setStatus(ConversionStatus.EXPIRED);
            conversionRepository.save(conversion);
            throw new RuntimeException("Conversion offer has expired! Please create a new one.");
        }

        return completeConversion(conversionId);
    }

    @Transactional
    public ConversionDto completeConversion(String conversionId) {
        Conversion conversion = conversionRepository.findById(conversionId)
                .orElseThrow(() -> new RuntimeException("Conversion not found!"));

        if (conversion.getStatus() != ConversionStatus.PENDING) {
            throw new RuntimeException("Conversion is already " + conversion.getStatus());
        }
        //- Costumer
        Account fromAccount = accountService.debitAccount(
                conversion.getUserId(),
                conversion.getFromAsset(),
                conversion.getFromAmount()
        );

        //+ Costumer
        Account toAccount = accountService.creditAccount(
                conversion.getUserId(),
                conversion.getToAsset(),
                conversion.getToAmount()
        );
        // + Company
        BigDecimal companyFee = conversion.getCompanyFee();
        if (companyFee != null && companyFee.compareTo(BigDecimal.ZERO) > 0) {
            accountService.creditCompanyAccount(
                    conversion.getFromAsset(),
                    companyFee,
                    conversionId
            );
            log.info("[CONVERSION] Company fee credited: {} {} (Ref: {})", companyFee, conversion.getFromAsset(), conversionId);
        }

        // - Company
        BigDecimal exchangeFee = conversion.getExchangeFee();
        if (exchangeFee != null && exchangeFee.compareTo(BigDecimal.ZERO) > 0) {
            accountService.creditCoinbaseAccount(
                    conversion.getFromAsset(),
                    exchangeFee,
                    conversionId
            );
            log.info("[CONVERSION] Exchange fee credited: {} {} (Ref: {})", exchangeFee, conversion.getFromAsset(), conversionId);
        }


        Transaction fromTx = transactionService.createTransaction(
                conversion.getUserId(),
                fromAccount.getId(),
                conversion.getFromAsset(),
                "CONVERSION_OUT",
                conversion.getFromAmount(),
                conversion.getRate(),
                conversionId,
                "Conversion out: " + conversion.getFromAmount() + " " + conversion.getFromAsset()
        );


        Transaction toTx = transactionService.createTransaction(
                conversion.getUserId(),
                toAccount.getId(),
                conversion.getToAsset(),
                "CONVERSION_IN",
                conversion.getToAmount(),
                conversion.getRate(),
                conversionId,
                "Conversion in: " + conversion.getToAmount() + " " + conversion.getToAsset()
        );

        // 6. Kullanıcı fee transaction kaydı (bilgi amaçlı)
        BigDecimal totalFees = conversion.getTotalFees();
        if (totalFees != null && totalFees.compareTo(BigDecimal.ZERO) > 0) {
            Transaction feeTx = transactionService.createTransaction(
                    conversion.getUserId(),
                    fromAccount.getId(),
                    conversion.getFromAsset(),
                    "FEE",
                    totalFees,
                    conversion.getRate(),
                    conversionId,
                    "Total fees: " + totalFees + " " + conversion.getFromAsset()
            );
        }

        // 7. Conversion'ı güncelle
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
        Conversion conversion = conversionRepository.findById(conversionId)
                .orElseThrow(() -> new RuntimeException("Conversion not found!"));

        if (!conversion.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access!");
        }

        if (conversion.getStatus() != ConversionStatus.PENDING) {
            throw new RuntimeException("Conversion is already " + conversion.getStatus());
        }

        conversion.setStatus(ConversionStatus.CANCELLED);
        conversionRepository.save(conversion);

        log.info("[CONVERSION] Cancelled: {}", conversionId);
        return conversionConverter.toDTO(conversion);
    }

    // ==================== SCHEDULED TASKS ====================

    @Scheduled(fixedDelay = 60000)
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
}
