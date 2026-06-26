package com.maf.exchangeapiv1.service;

import com.maf.exchangeapiv1.dto.AccountDto;
import com.maf.exchangeapiv1.dto.converter.AccountConverter;
import com.maf.exchangeapiv1.model.Account;
import com.maf.exchangeapiv1.model.User;
import com.maf.exchangeapiv1.repository.AccountRepository;
import com.maf.exchangeapiv1.repository.UserRepository;
import com.maf.exchangeapiv1.thirdPartAPIGateWay.BinanceService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AccountConverter accountConverter;
    private final BinanceService binanceService;
    private final String COMPANY_USER_ID = "company-user-id";
    private final String COINBASE_USER_ID = "coinbase-system-id";


    public List<AccountDto> getUserAccounts(String userId) {
        return accountConverter.toDTOList(accountRepository.findByUserId(userId));
    }

    public AccountDto getAccount(String userId, String asset) {
         Account account = getAccountById(userId,asset);
         return accountConverter.toDTO(account);
    }

    public BigDecimal getBalance(String userId, String asset) {
        return accountRepository.findByUserIdAndAsset(userId, asset)
                .map(Account::getBalance)
                .orElse(BigDecimal.ZERO);
    }

    @Transactional
    public AccountDto createAccount(String userId, String asset) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User Not Found!"));
        Account account = accountRepository.findByUserIdAndAsset(userId, asset.toUpperCase())
                .orElseGet(() -> {
                    Account newAccount = Account.builder()
                            .user(user)
                            .asset(asset.toUpperCase())
                            .balance(BigDecimal.ZERO)
                            .lockedBalance(BigDecimal.ZERO)
                            .availableBalance(BigDecimal.ZERO)
                            .build();
                    log.info("[ACCOUNT] New account has been created: {} - {}", userId, asset.toUpperCase());
                     accountRepository.save(newAccount);
                    return newAccount;
                });
        return accountConverter.toDTO(account);
    }


    @Transactional
    public AccountDto deposit(String userId, String asset, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be greater than zero!");
        }

        Account account = accountRepository.findByUserIdAndAsset(userId, asset.toUpperCase())
                .orElseThrow(()-> new RuntimeException("Account not found!"));

        BigDecimal oldBalance = account.getBalance();
        account.setBalance(account.getBalance().add(amount));
        account.setAvailableBalance(account.getBalance().subtract(account.getLockedBalance()));

        Account savedAccount = accountRepository.save(account);

        log.info("[DEPOSIT] {} - {}: {} {} (Old: {}, New: {})",
                userId, asset, amount, description != null ? "(" + description + ")" : "",
                oldBalance, savedAccount.getBalance());
        return accountConverter.toDTO(savedAccount);
    }

    @Transactional
    public Account creditCompanyAccount(String asset, BigDecimal amount, String referenceId) {
        Account companyAccount = accountRepository.findByUserIdAndAsset(COMPANY_USER_ID, asset)
                .orElseThrow(() -> new RuntimeException("Company " + asset + " account not found!"));
        companyAccount.setBalance(companyAccount.getBalance().add(amount));
        companyAccount.setAvailableBalance(companyAccount.getBalance().subtract(companyAccount.getLockedBalance()));
        log.info("[ACCOUNT] Company credited: +{} {} (Ref: {})", amount, asset, referenceId);
        return accountRepository.save(companyAccount);
    }

    @Transactional
    public Account creditCoinbaseAccount(String asset, BigDecimal amount, String referenceId) {
        Account coinbaseAccount = accountRepository.findByUserIdAndAsset(COINBASE_USER_ID, asset)
                .orElseThrow(() -> new RuntimeException("Coinbase " + asset + " account not found!"));
        coinbaseAccount.setBalance(coinbaseAccount.getBalance().add(amount));
        coinbaseAccount.setAvailableBalance(coinbaseAccount.getBalance().subtract(coinbaseAccount.getLockedBalance()));
        log.info("[ACCOUNT] Coinbase credited: +{} {} (Ref: {})", amount, asset, referenceId);
        return accountRepository.save(coinbaseAccount);
    }

    public Account getAccountById(String userId, String asset) {
        return accountRepository.findByUserIdAndAsset(userId, asset)
                .orElseThrow(() -> new RuntimeException(asset + " account not found for user " + userId));
    }

    @Transactional
    public Account debitAccount(String userId, String asset, BigDecimal amount) {
        Account account = getAccountById(userId, asset);
        if (account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance in " + asset + " account!");
        }
        account.setBalance(account.getBalance().subtract(amount));
        account.setAvailableBalance(account.getBalance().subtract(account.getLockedBalance()));
        return accountRepository.save(account);
    }

    @Transactional
    public Account creditAccount(String userId, String asset, BigDecimal amount) {
        Account account = getAccountById(userId, asset);
        account.setBalance(account.getBalance().add(amount));
        account.setAvailableBalance(account.getBalance().subtract(account.getLockedBalance()));
        return accountRepository.save(account);
    }

    public List<AccountDto> getThirdPartAccountList(String UserId) {
        return binanceService.getAccountList(UserId);
    }



}
