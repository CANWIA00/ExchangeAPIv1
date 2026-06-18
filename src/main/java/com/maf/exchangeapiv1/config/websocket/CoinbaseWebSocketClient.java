package com.maf.exchangeapiv1.config.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.maf.exchangeapiv1.cache.OrderBookCache;
import com.maf.exchangeapiv1.handler.MessageHandler;
import com.maf.exchangeapiv1.handler.MessageHandlerFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
public class CoinbaseWebSocketClient {
    private static final String WS_URL = "wss://ws-feed-public.sandbox.exchange.coinbase.com";
    private final MessageHandlerFactory handlerFactory;
    private final OrderBookCache cache;
    private final ReactorNettyWebSocketClient webSocketClient;
    private final ObjectMapper mapper;
    private final CoinbaseWsAuth auth;
    private WebSocketSession session;

    public CoinbaseWebSocketClient(MessageHandlerFactory handlerFactory, OrderBookCache cache, ReactorNettyWebSocketClient webSocketClient, CoinbaseWsAuth auth, ObjectMapper mapper) {
        this.handlerFactory = handlerFactory;
        this.cache = cache;
        this.webSocketClient = webSocketClient;
        this.auth = auth;
        this.mapper = mapper;
    }

    @Async
    @PostConstruct
    public void connect() {

        List<String> productIds = List.of("BTC-USD");

        try {
            String subscribeMessage = buildSubscribeMessage(productIds);
            log.info("[WS] Connection is Started-> " + WS_URL);
            webSocketClient.execute(URI.create(WS_URL), session -> {
                this.session = session;
                log.info("[WS] Session is created");
                return session.send(Flux.just(subscribeMessage).map(session::textMessage))
                        .thenMany(session.receive()
                                .map(WebSocketMessage::getPayloadAsText))
                        .doOnNext(this::handleIncomingMessage)
                        .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(2))
                                .maxBackoff(Duration.ofSeconds(30))
                                .doBeforeRetry(rs -> log.info("[WS] Reconnecting... (Attempt: {})", rs.failure())))
                        .then();
            }).subscribe(
                    null,
                    error -> log.error("[WS] Subscription Error: {}", error.getMessage())
            );
        } catch (Exception e) {
            log.info("[WS INIT ERROR] {}", e.getMessage());
        }
    }

    private String buildSubscribeMessage(List<String> productIds) throws Exception {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());

        String signature = auth.createSignature(timestamp);
        ObjectNode root = mapper.createObjectNode();

        root.put("type", "subscribe");

        ArrayNode products = root.putArray("product_ids");

        productIds.forEach(products::add);

        ArrayNode channels = root.putArray("channels");

        channels.add("ticker");
        channels.add("level2");
        channels.add("heartbeat");

        root.put("signature", signature);
        root.put("key", auth.getApiKey());
        root.put("passphrase", auth.getPassphrase());
        root.put("timestamp", timestamp);

        return mapper.writeValueAsString(root);
    }

    private void handleIncomingMessage(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            String type = root.path("type").asText();
            String productId = root.path("product_id").asText();
            MessageHandler handler = handlerFactory.getHandler(type);
            handler.handle(root, productId);

        } catch (Exception e) {
            log.info("[WS] Mesaj işleme hatası {}", String.valueOf(e));
        }
    }
}
