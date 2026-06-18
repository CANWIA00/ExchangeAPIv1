package com.maf.exchangeapiv1.config.webClient;

import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

public class CoinbaseWcSignatureFilter implements ExchangeFilterFunction {

    private final String apiKey;
    private final String apiSecret;
    private final String apiPassphrase;

    public CoinbaseWcSignatureFilter(String apiKey, String apiSecret, String apiPassphrase) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.apiPassphrase = apiPassphrase;
    }

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String method = request.method().name();

        String path = request.url().getRawPath();
        if (request.url().getRawQuery() != null) {
            path += "?" + request.url().getRawQuery();
        }

        String body = "";

        String signature = generateSignature(timestamp, method, path, body);
        ClientRequest signedRequest = ClientRequest.from(request)
                .header("cb-access-key", this.apiKey)
                .header("cb-access-sign", signature)
                .header("cb-access-timestamp", timestamp)
                .header("cb-access-passphrase", this.apiPassphrase)
                .header("Content-Type", "application/json")
                .build();

        return next.exchange(signedRequest);
    }

    private String generateSignature(String timestamp, String method, String path, String body) {
        try {
            // Exchange API Standart İmza Formatı: timestamp + METHOD + path + body
            String message = timestamp + method + path + body;
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");

            // EN KRİTİK NOKTA: Coinbase Exchange API secret anahtarını Base64 formatında üretir.
            // Direkt .getBytes() yapmak yerine önce Base64 şifresini çözüp byte dizisini elde etmeliyiz.
            byte[] decodedSecret = Base64.getDecoder().decode(apiSecret);
            SecretKeySpec secret_key = new SecretKeySpec(decodedSecret, "HmacSHA256");
            sha256_HMAC.init(secret_key);

            byte[] hash = sha256_HMAC.doFinal(message.getBytes(StandardCharsets.UTF_8));

            // Çıkan Hmac sonucu tekrar standart Base64 metni haline getirilir.
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Exchange API Signature generation failed", e);
        }
    }

}
