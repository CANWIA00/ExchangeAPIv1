package com.maf.exchangeapiv1.handler;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MessageHandlerFactory {

    private final Map<String, MessageHandler> handlers = new ConcurrentHashMap<>();

    public MessageHandlerFactory(
            TickerHandler tickerHandler,
            SnapshotHandler snapshotHandler,
            L2UpdateHandler l2UpdateHandler,
            ErrorHandler errorHandler,
            SubscriptionsHandler subscriptionsHandler,
            DefaultHandler defaultHandler) {

        handlers.put("ticker", tickerHandler);
        handlers.put("snapshot", snapshotHandler);
        handlers.put("l2update", l2UpdateHandler);
        handlers.put("error", errorHandler);
        handlers.put("subscriptions", subscriptionsHandler);
        handlers.put("default", defaultHandler);
    }

    public MessageHandler getHandler(String type) {
        return handlers.getOrDefault(type, handlers.get("default"));
    }
}
