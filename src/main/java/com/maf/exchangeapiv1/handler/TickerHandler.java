package com.maf.exchangeapiv1.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.maf.exchangeapiv1.cache.OrderBookCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
public class TickerHandler implements MessageHandler {

    private final OrderBookCache orderBookCache;

    public TickerHandler(OrderBookCache orderBookCache) {
        this.orderBookCache = orderBookCache;
    }

    @Override
    public void handle(JsonNode root, String productId) {
        if (root.has("price")) {
            BigDecimal price = new BigDecimal(root.get("price").asText());
            orderBookCache.updatePrice(productId, price);
            log.info("[WS] Ticker {}: {}", productId, price);
        }
    }
}
