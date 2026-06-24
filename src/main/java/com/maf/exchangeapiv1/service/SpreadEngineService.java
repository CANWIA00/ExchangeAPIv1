package com.maf.exchangeapiv1.service;

import com.maf.exchangeapiv1.cache.OrderBookCache;
import com.maf.exchangeapiv1.dto.FinalPriceDto;
import com.maf.exchangeapiv1.scheduler.FeeManagementScheduler;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class SpreadEngineService {

    private final FeeManagementScheduler scheduler;
    private final OrderBookCache orderBookCache;
    private final SlippageService slippageService;
    private final BigDecimal COMPANY_PROFIT_MARGIN = new BigDecimal("0.002");

    public SpreadEngineService(FeeManagementScheduler scheduler, OrderBookCache orderBookCache, SlippageService slippageService) {
        this.scheduler = scheduler;
        this.orderBookCache = orderBookCache;
        this.slippageService = slippageService;
    }

    private BigDecimal calculateMultiplier(BigDecimal exchangeFee, BigDecimal slippage, boolean isBuy) {
        if (isBuy) {
            return BigDecimal.ONE.add(exchangeFee)
                    .multiply(BigDecimal.ONE.add(COMPANY_PROFIT_MARGIN))
                    .multiply(BigDecimal.ONE.add(slippage));
        } else {
            return BigDecimal.ONE.subtract(exchangeFee)
                    .multiply(BigDecimal.ONE.subtract(COMPANY_PROFIT_MARGIN))
                    .multiply(BigDecimal.ONE.subtract(slippage));
        }
    }


     // Base Buy/Sell
    public FinalPriceDto calculateBaseFinalPrice(BigDecimal amount, String pair, String side) {
        boolean isBuy = side.equalsIgnoreCase("buy");
        String operation = isBuy ? "BUY" : "SELL";

        BigDecimal spotPrice = isBuy ? orderBookCache.getBestAsk(pair) : orderBookCache.getBestBid(pair);
        BigDecimal avgPrice = orderBookCache.getAveragePriceWithVolume(pair, amount, isBuy);
        BigDecimal exchangeFeeRate = scheduler.getFeeRate();
        BigDecimal exchangeFee = spotPrice.multiply(exchangeFeeRate);
        BigDecimal companyFee = spotPrice.multiply(COMPANY_PROFIT_MARGIN);
        BigDecimal slippageRate = BigDecimal.ZERO;
        BigDecimal slippageAmount = BigDecimal.ZERO;
        BigDecimal multiplier = calculateMultiplier(exchangeFeeRate, BigDecimal.ZERO, isBuy);
        BigDecimal unitPrice = avgPrice.multiply(multiplier);
        BigDecimal totalCost = unitPrice.multiply(amount);

        return FinalPriceDto.builder()
                .inputAmount(amount)
                .outputAmount(amount)
                .spotPrice(spotPrice)
                .averagePrice(avgPrice)
                .exchangeFee(exchangeFee)
                .exchangeFeeRate(exchangeFeeRate)
                .companyFee(companyFee)
                .companyFeeRate(COMPANY_PROFIT_MARGIN)
                .slippageAmount(slippageAmount)
                .slippageRate(slippageRate)
                .unitPrice(unitPrice)
                .totalCost(totalCost)
                .multiplier(multiplier)
                .side(operation)
                .pair(pair)
                .build();
    }


     // Quote Buy/Sell
    public FinalPriceDto calculateQuoteFinalPrice(BigDecimal targetAmount, String pair, String side) {
        boolean isBuy = side.equalsIgnoreCase("buy");
        String operation = isBuy ? "BUY" : "SELL";

        BigDecimal spotPrice = isBuy ? orderBookCache.getBestAsk(pair) : orderBookCache.getBestBid(pair);
        if (spotPrice == null || spotPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("No price available for " + pair);
        }
        BigDecimal estimatedAmount = targetAmount.divide(spotPrice, 8, RoundingMode.HALF_UP);
        BigDecimal slippageRate = slippageService.calculateSlippage(pair, estimatedAmount, isBuy);
        BigDecimal slippageAmount = spotPrice.multiply(slippageRate);
        BigDecimal exchangeFeeRate = scheduler.getFeeRate();
        BigDecimal exchangeFee = spotPrice.multiply(exchangeFeeRate);
        BigDecimal companyFee = spotPrice.multiply(COMPANY_PROFIT_MARGIN);
        BigDecimal multiplier = calculateMultiplier(exchangeFeeRate, slippageRate, isBuy);
        BigDecimal unitPrice = spotPrice.multiply(multiplier);
        BigDecimal outputAmount = targetAmount.divide(unitPrice, 8, RoundingMode.HALF_DOWN);
        BigDecimal totalCost = targetAmount;

        return FinalPriceDto.builder()
                .inputAmount(targetAmount)
                .outputAmount(outputAmount)
                .spotPrice(spotPrice)
                .averagePrice(unitPrice)
                .exchangeFee(exchangeFee)
                .exchangeFeeRate(exchangeFeeRate)
                .companyFee(companyFee)
                .companyFeeRate(COMPANY_PROFIT_MARGIN)
                .slippageAmount(slippageAmount)
                .slippageRate(slippageRate)
                .unitPrice(unitPrice)
                .totalCost(totalCost)
                .multiplier(multiplier)
                .side(operation)
                .pair(pair)
                .build();
    }
}
