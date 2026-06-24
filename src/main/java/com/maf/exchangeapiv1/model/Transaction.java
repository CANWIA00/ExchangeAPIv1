package com.maf.exchangeapiv1.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String asset;

    @Column(nullable = false)
    private String type;  // DEPOSIT, WITHDRAW, BUY, SELL, CONVERSION_OUT, CONVERSION_IN, FEE

    private BigDecimal amount;

    private BigDecimal unitPrice;

    private BigDecimal totalCost;

    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;

    private String referenceId;  // Order ID, Transaction ID, Conversion ID

    private String status;  // PENDING, COMPLETED, FAILED

    private String description;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = "COMPLETED";
        }
    }
}
