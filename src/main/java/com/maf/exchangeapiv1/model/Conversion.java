package com.maf.exchangeapiv1.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "conversions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String fromAsset;

    @Column(nullable = false)
    private String toAsset;

    @Column(nullable = false)
    private BigDecimal fromAmount;

    @Column(nullable = false)
    private BigDecimal toAmount;

    @Column(nullable = false)
    private BigDecimal rate;

    private String fromAccountId;

    private String toAccountId;

    private String transactionId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ConversionStatus status;

    private String referenceId;

    private String description;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    // Commission
    private BigDecimal exchangeFee;
    private BigDecimal companyFee;
    private BigDecimal slippageFee;
    private BigDecimal totalFees;


    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        expiresAt = createdAt.plusSeconds(30);
        if (status == null) {
            status = ConversionStatus.PENDING;
        }
    }
}