package com.maf.exchangeapiv1.service;

import com.maf.exchangeapiv1.model.Transaction;
import com.maf.exchangeapiv1.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@AllArgsConstructor
@Slf4j
public class TransactionService {
    private final TransactionRepository transactionRepository;

    @Transactional
    public Transaction createTransaction(String userId, String accountId, String asset,
                                         String type, BigDecimal amount, BigDecimal price,
                                         String referenceId, String description) {

        BigDecimal total = amount.multiply(price != null ? price : BigDecimal.ONE);

        Transaction transaction = Transaction.builder()
                .userId(userId)
                .accountId(accountId)
                .asset(asset)
                .type(type)
                .amount(amount)
                .unitPrice(price)
                .totalCost(total)
                .referenceId(referenceId)
                .status("COMPLETED")
                .description(description)
                .build();

        Transaction saved = transactionRepository.save(transaction);
        log.info("[TRANSACTION] {} - {}: {} {} (Ref: {})", type, userId, amount, asset, referenceId);
        return saved;
    }
}
