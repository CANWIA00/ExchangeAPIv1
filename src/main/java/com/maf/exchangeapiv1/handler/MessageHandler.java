package com.maf.exchangeapiv1.handler;
import com.fasterxml.jackson.databind.JsonNode;

public interface MessageHandler {
    void handle(JsonNode root, String productId);
}
