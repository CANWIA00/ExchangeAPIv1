package com.maf.exchangeapiv1.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.maf.exchangeapiv1.model.ConversionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversionDto {

    private String id;
    private String userId;
    private String fromAccountId;
    private String fromAsset;
    private BigDecimal fromAmount;

    private String toAccountId;
    private String toAsset;
    private BigDecimal toAmount;

    private BigDecimal rate;
    @JsonIgnore
    private String transactionId;
    private ConversionStatus status;
    @JsonIgnore
    private String referenceId;
    private String description;
    private LocalDateTime createdAt;
    @JsonIgnore
    private LocalDateTime completedAt;
}
