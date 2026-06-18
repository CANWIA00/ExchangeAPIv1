package com.maf.exchangeapiv1.handler;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DefaultHandler implements MessageHandler {


    @Override
    public void handle(JsonNode root, String productId) {
        String type = root.path("type").asText();
        log.warn("[WS] Unknown message type: {}", type);
    }
}
