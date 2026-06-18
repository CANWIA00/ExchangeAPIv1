package com.maf.exchangeapiv1.cache;

import com.maf.exchangeapiv1.exception.InsufficientLiquidityException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
@Service
public class OrderBookCache {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String PRICE_KEY = "price:%s";
    private static final String ASKS_KEY = "orderbook:asks:%s";
    private static final String BIDS_KEY = "orderbook:bids:%s";
    private static final String ASKS_SIZE_KEY = "orderbook:asks:%s:sizes";
    private static final String BIDS_SIZE_KEY = "orderbook:bids:%s:sizes";

    public OrderBookCache(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ==================== PRICE METHODS (Ticker) ====================

    public void updatePrice(String pair, BigDecimal price) {
        String key = String.format(PRICE_KEY, pair);
        redisTemplate.opsForValue().set(key, price.toString());
        System.out.println("[REDIS] Price updated - " + key + " = " + price);
    }

    public BigDecimal getSelectedPriceTicker(String pair) {
        String key = String.format(PRICE_KEY, pair);
        String priceStr = redisTemplate.opsForValue().get(key);
        if (priceStr == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(priceStr);
        } catch (NumberFormatException e) {
            System.err.println("[REDIS] Price parse error: " + priceStr);
            return BigDecimal.ZERO;
        }
    }

    public Map<String, BigDecimal> getAllPricesTicker() {
        Map<String, BigDecimal> result = new HashMap<>();
        String pattern = "price:*";
        Set<String> keys = redisTemplate.keys(pattern);

        if (keys != null) {
            for (String key : keys) {
                String priceStr = redisTemplate.opsForValue().get(key);
                if (priceStr != null) {
                    // key = "price:BTC-USD" -> "BTC-USD"
                    String pair = key.substring(6);
                    result.put(pair, new BigDecimal(priceStr));
                }
            }
        }
        return result;
    }

    // ==================== ORDER BOOK METHODS ====================

    public BigDecimal getBestBid(String pair) {
        String key = String.format(BIDS_KEY, pair);
        // Max to Min
        Set<String> bids = redisTemplate.opsForZSet().reverseRange(key, 0, 0);
        if (bids == null || bids.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(bids.iterator().next());
    }

    public BigDecimal getBestAsk(String pair) {
        String key = String.format(ASKS_KEY, pair);
        //Min to max
        Set<String> asks = redisTemplate.opsForZSet().range(key, 0, 0);
        if (asks == null || asks.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(asks.iterator().next());
    }

    public void updateOrderBook(String pair, String side, String price, String size) {
        BigDecimal p = new BigDecimal(price);

        redisTemplate.opsForZSet().add(getKey(pair, side), price, p.doubleValue());
        redisTemplate.opsForHash().put(getSizeKey(pair, side), price, size);

        System.out.println("[REDIS] OrderBook updated - " + side + " " + price + " : " + size);
    }

    public void removePriceLevel(String pair, String side, String price) {
        redisTemplate.opsForZSet().remove(getKey(pair, side), price);
        redisTemplate.opsForHash().delete(getSizeKey(pair, side), price);

        System.out.println("[REDIS] Price level removed - " + side + " " + price);
    }
    //********* Helper for update and remove
    private boolean isBidSide(String side) {
        return "buy".equalsIgnoreCase(side) || "bid".equalsIgnoreCase(side);
    }

    private String getKey(String pair, String side) {
        return isBidSide(side) ? String.format(BIDS_KEY, pair) : String.format(ASKS_KEY, pair);
    }

    private String getSizeKey(String pair, String side) {
        return isBidSide(side) ? String.format(BIDS_SIZE_KEY, pair) : String.format(ASKS_SIZE_KEY, pair);
    }


    // ==================== COST CALCULATION METHODS ====================

    public BigDecimal getAveragePriceWithVolume(String pair, BigDecimal amount, boolean isBuy) {
        BigDecimal totalCost = calculateCost(pair, amount, isBuy);
        return totalCost.divide(amount, 8, RoundingMode.HALF_UP);
    }

    public BigDecimal getTotalCostForAmount(String pair, BigDecimal amount, boolean isBuy) {
        return calculateCost(pair, amount, isBuy);
    }

    private BigDecimal calculateCost(String pair, BigDecimal amount, boolean isBuy) {
        String key;
        String sizeKey;

        if (isBuy) {
            key = String.format(ASKS_KEY, pair);
            sizeKey = String.format(ASKS_SIZE_KEY, pair);
        } else {
            key = String.format(BIDS_KEY, pair);
            sizeKey = String.format(BIDS_SIZE_KEY, pair);
        }

        Set<String> prices;
        if (isBuy) {
            //min to max
            prices = redisTemplate.opsForZSet().range(key, 0, -1);
        } else {
            //max to min
            prices = redisTemplate.opsForZSet().reverseRange(key, 0, -1);
        }

        if (prices == null || prices.isEmpty()) {
            throw new InsufficientLiquidityException("There is insufficient liquidity for " + pair);
        }

        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal remainingAmount = amount;

        for (String priceStr : prices) {

            Object sizeObj = redisTemplate.opsForHash().get(sizeKey, priceStr);
            if (sizeObj == null) continue;

            BigDecimal price = new BigDecimal(priceStr);
            BigDecimal availableQuantity = new BigDecimal(sizeObj.toString());

            BigDecimal takenAmount = remainingAmount.min(availableQuantity);
            totalCost = totalCost.add(price.multiply(takenAmount));
            remainingAmount = remainingAmount.subtract(takenAmount);

            if (remainingAmount.compareTo(BigDecimal.ZERO) == 0) break;
        }

        if (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
            throw new InsufficientLiquidityException("Insufficient liquidity to cover the requested amount!");
        }

        System.out.println("[REDIS] Cost calculated - " + amount + " " + pair + " = " + totalCost);

        return totalCost.setScale(8, RoundingMode.HALF_UP);
    }
}


