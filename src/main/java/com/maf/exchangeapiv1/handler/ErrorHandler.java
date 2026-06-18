package com.maf.exchangeapiv1.handler;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ErrorHandler implements MessageHandler {


    @Override
    public void handle(JsonNode root, String productId) {
        log.error("[WS] Coinbase Error: {}", root.toPrettyString());
    }
}
