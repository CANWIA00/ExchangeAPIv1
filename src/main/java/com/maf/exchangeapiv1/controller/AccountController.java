package com.maf.exchangeapiv1.controller;

import com.maf.exchangeapiv1.dto.AccountDto;
import com.maf.exchangeapiv1.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/wallets")
    public ResponseEntity<List<AccountDto>> getAllAccounts(Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(accountService.getUserAccounts(userId));
    }

    @GetMapping("/wallets/thirdPart")
    public ResponseEntity<List<AccountDto>> getBinanceAccounts(Authentication authentication) {
        String userId = authentication.getName();
        List<AccountDto> accounts = accountService.getThirdPartAccountList(userId);
        return ResponseEntity.ok(accounts);
    }


    @GetMapping("/wallet/{asset}")
    public ResponseEntity<AccountDto> getAccount(Authentication authentication, @PathVariable String asset) {
        String userId = authentication.getName();
        return ResponseEntity.ok(accountService.getAccount(userId, asset.toUpperCase()));
    }

    @GetMapping("/balance")
    public ResponseEntity<BigDecimal> getBalance(Authentication authentication, @RequestParam String asset) {
        String userId = authentication.getName();
        return ResponseEntity.ok(accountService.getBalance(userId, asset.toUpperCase()));
    }

    @PostMapping("/create")
    public ResponseEntity<AccountDto> createAccount(Authentication authentication, @RequestParam String asset) {
        String userId = authentication.getName();
        AccountDto accountDto = accountService.createAccount(userId, asset.toUpperCase());
        return ResponseEntity.ok(accountDto);
    }

    @PostMapping("/deposit/{description}")
    public ResponseEntity<AccountDto> deposit(Authentication authentication, @RequestParam String asset, @RequestParam BigDecimal amount, @RequestParam String description) {
        String userId = authentication.getName();
        AccountDto account = accountService.deposit(userId,asset,amount,description);
        return ResponseEntity.ok(account);
    }


}
