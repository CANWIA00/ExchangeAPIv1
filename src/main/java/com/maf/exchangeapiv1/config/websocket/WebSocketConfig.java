package com.maf.exchangeapiv1.config.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.WebsocketClientSpec;


@Configuration
public class WebSocketConfig {

    @Bean
    public ReactorNettyWebSocketClient reactorNettyWebSocketClient() {
        HttpClient httpClient = HttpClient.create();

        return new ReactorNettyWebSocketClient(httpClient, () ->
                WebsocketClientSpec.builder().maxFramePayloadLength(10 * 1024 * 1024)
        );
    }
}
