package com.maf.exchangeapiv1.service;

import com.maf.exchangeapiv1.cache.OrderBookCache;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class SlippageService {
    private final OrderBookCache orderBookCache;

    public SlippageService(OrderBookCache orderBookCache) {
        this.orderBookCache = orderBookCache;
    }

    //(Costumer avg price - best price) / best price
    public BigDecimal calculateSlippage(String pair, BigDecimal amount, boolean isBuy) {
        BigDecimal bestPrice = isBuy ? orderBookCache.getBestAsk(pair) : orderBookCache.getBestBid(pair);
        BigDecimal avgPrice = orderBookCache.getAveragePriceWithVolume(pair, amount, isBuy);
        BigDecimal slippage = avgPrice.subtract(bestPrice).divide(bestPrice, 6, RoundingMode.HALF_UP).abs();
        System.out.println("Slippage: " + slippage);

        return slippage.max(new BigDecimal("0.0005")).min(new BigDecimal("0.05"));
    }
}
