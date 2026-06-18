package com.maf.exchangeapiv1.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.maf.exchangeapiv1.cache.OrderBookCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
@Slf4j
@Component
public class L2UpdateHandler implements MessageHandler{
    private final OrderBookCache cache;

    public L2UpdateHandler(OrderBookCache cache) {
        this.cache = cache;
    }

    @Override
    public void handle(JsonNode root, String productId) {
        JsonNode changes = root.path("changes");

        for (JsonNode change : changes) {
            String side = change.get(0).asText();
            String price = change.get(1).asText();
            String size = change.get(2).asText();

            if (BigDecimal.ZERO.compareTo(new BigDecimal(size)) == 0) {
                cache.removePriceLevel(productId, side, price);
            } else {
                cache.updateOrderBook(productId, side, price, size);
            }
        }

        log.debug("[WS] L2Update {}: {} changes", productId, changes.size());
    }
}
