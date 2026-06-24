package com.maf.exchangeapiv1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinalPriceDto {


    private BigDecimal inputAmount;      // User Input Amount
    private BigDecimal outputAmount;     // User Income Amount

    private BigDecimal spotPrice;        // The best StockMarket Price
    private BigDecimal averagePrice;     // Avg Price (with slippage)

    // commissions
    private BigDecimal exchangeFee;
    private BigDecimal exchangeFeeRate;
    private BigDecimal companyFee;
    private BigDecimal companyFeeRate;
    private BigDecimal slippageAmount;
    private BigDecimal slippageRate;

    // Final Cost
    private BigDecimal unitPrice;        // Last price for one unit (with commissions)
    private BigDecimal totalCost;        // Total Cost (unitPrice × inputAmount)

    private BigDecimal multiplier;       // Multiplier for all commissions

    private String side;                 // BUY / SELL
    private String pair;                 // asset pair

    public BigDecimal getTotalFees() {
        BigDecimal exchange = exchangeFee != null ? exchangeFee : BigDecimal.ZERO;
        BigDecimal company = companyFee != null ? companyFee : BigDecimal.ZERO;
        return exchange.add(company);
    }
}
