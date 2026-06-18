package com.maf.exchangeapiv1.service;

import com.maf.exchangeapiv1.cache.OrderBookCache;
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
        if (isBuy) { // multiplier = (1 + fee) × (1 + kar) × (1 + slippage)
            return BigDecimal.ONE.add(exchangeFee)
                    .multiply(BigDecimal.ONE.add(COMPANY_PROFIT_MARGIN))
                    .multiply(BigDecimal.ONE.add(slippage));
        } else { // multiplier = (1 - fee) × (1 - kar) × (1 - slippage)
            return BigDecimal.ONE.subtract(exchangeFee)
                    .multiply(BigDecimal.ONE.subtract(COMPANY_PROFIT_MARGIN))
                    .multiply(BigDecimal.ONE.subtract(slippage));
        }
    }

    public BigDecimal calculateBaseFinalPrice(BigDecimal amount, String pair, String side) {
        boolean isBuy = side.equalsIgnoreCase("buy");
        BigDecimal avgPrice = orderBookCache.getAveragePriceWithVolume(pair, amount, isBuy);
        // Slippage already existed in averagePrice
        BigDecimal multiplier = calculateMultiplier(scheduler.getFeeRate(), BigDecimal.ZERO, isBuy);
        System.out.println("**************************************** Calculate Base FinalPrice **************************************");
        System.out.println("Desired amount of coin: "+ amount + " " + pair);
        System.out.println("AvgPrice without any fee: " + avgPrice);
        System.out.println("multiplier(Fees): " + multiplier);
        System.out.println("New Price: " + avgPrice.multiply(multiplier).multiply(amount).setScale(2, RoundingMode.HALF_UP));
        return avgPrice.multiply(multiplier).multiply(amount).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateQuoteFinalPrice(BigDecimal targetAmount, String pair, String side) {
        boolean isBuy = side.equalsIgnoreCase("buy");
        BigDecimal bestPrice = isBuy ? orderBookCache.getBestAsk(pair) : orderBookCache.getBestBid(pair);

        // EstimatedAmount = targetAmountPrice/bestPrice
        BigDecimal estimatedAmount = targetAmount.divide(bestPrice, 8, RoundingMode.HALF_UP);
        // Slippage rate
        BigDecimal totalSlippage = slippageService.calculateSlippage(pair, estimatedAmount, isBuy);
        BigDecimal multiplier = calculateMultiplier(scheduler.getFeeRate(), totalSlippage, isBuy);

        System.out.println("**************************************** Calculate Quote FinalPrice **************************************");
        System.out.println("Desired amount of coin: " + targetAmount + " Dollar");
        System.out.println("TotalSlippage (fee): " + totalSlippage);
        System.out.println("multiplier (fees): " + multiplier);
        System.out.println("New Price: " + targetAmount.divide(bestPrice.multiply(multiplier), 8, RoundingMode.HALF_DOWN));
        return targetAmount.divide(bestPrice.multiply(multiplier), 8, RoundingMode.HALF_DOWN);
    }
}
