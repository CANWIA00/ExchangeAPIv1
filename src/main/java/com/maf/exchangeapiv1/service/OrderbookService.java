package com.maf.exchangeapiv1.service;

import com.maf.exchangeapiv1.cache.OrderBookCache;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class OrderbookService {

    private final OrderBookCache orderBookCache;

    public Flux<Map<String, BigDecimal>> streamPrices() {
        return Flux.interval(Duration.ofMillis(500)).map(tick -> orderBookCache.getAllPricesTicker());
    }

    public OrderbookService(OrderBookCache orderBookCache) {
        this.orderBookCache = orderBookCache;
    }

    public Map<String, BigDecimal> getAllPricesTicker() {
        return orderBookCache.getAllPricesTicker();
    }

    public BigDecimal getSelectedPriceTicker(String pair) {
        return orderBookCache.getSelectedPriceTicker(pair);
    }

    public Map<String, BigDecimal> getBestPrice(String pair){
        Map<String, BigDecimal> response = new HashMap<>();
        response.put("sellPrice", orderBookCache.getBestBid(pair));
        response.put("buyPrice", orderBookCache.getBestAsk(pair));
        return response;
    }

    public BigDecimal getBestPriceWithVolume(String pair, BigDecimal amount, String side) {
        boolean isBuy = "BUY".equalsIgnoreCase(side);
        return orderBookCache.getAveragePriceWithVolume(pair, amount, isBuy);
    }

    public Map<String, BigDecimal> getOrderTotalPrice( String pair,String side, BigDecimal amount) {
        boolean isBuy = "BUY".equalsIgnoreCase(side);

        BigDecimal avgPrice = orderBookCache.getAveragePriceWithVolume(pair, amount, isBuy);
        BigDecimal totalCost = orderBookCache.getTotalCostForAmount(pair, amount, isBuy);

        Map<String, BigDecimal> response = new HashMap<>();
        response.put("averagePrice", avgPrice);
        response.put("totalCost", totalCost);
        return response;
    }



}
