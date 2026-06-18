package com.maf.exchangeapiv1.config.webClient;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient publicCoinbaseWebClient(WebClient.Builder builder) {
        return builder.baseUrl("https://api-public.sandbox.exchange.coinbase.com")
                .build();
    }

    @Bean
    public WebClient privateWebClient(WebClient.Builder builder) {
        String apiKey = System.getenv("API_KEY");
        String apiSecret = System.getenv("API_SECRET");
        String apiPassphrase = System.getenv("API_PASSPHRASE");

        return builder.baseUrl("https://api-public.sandbox.exchange.coinbase.com")
                .filter(new CoinbaseWcSignatureFilter(apiKey, apiSecret, apiPassphrase))
                .build();
    }
}
