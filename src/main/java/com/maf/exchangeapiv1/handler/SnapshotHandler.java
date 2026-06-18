package com.maf.exchangeapiv1.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.maf.exchangeapiv1.cache.OrderBookCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
@Slf4j
@Component
public class SnapshotHandler implements MessageHandler {

    private final OrderBookCache cache;

    public SnapshotHandler(OrderBookCache cache) {
        this.cache = cache;
    }

    @Override
    public void handle(JsonNode root, String productId) {
        JsonNode bids = root.path("bids");
        JsonNode asks = root.path("asks");

        for (JsonNode bid : bids) {
            cache.updateOrderBook(productId, "buy", bid.get(0).asText(), bid.get(1).asText());
        }
        for (JsonNode ask : asks) {
            cache.updateOrderBook(productId, "sell", ask.get(0).asText(), ask.get(1).asText());
        }

        log.debug("[WS] Snapshot {}: {} bid, {} ask", productId, bids.size(), asks.size());
    }
}
