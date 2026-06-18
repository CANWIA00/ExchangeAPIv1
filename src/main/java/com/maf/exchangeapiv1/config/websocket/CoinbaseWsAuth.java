package com.maf.exchangeapiv1.config.websocket;

import lombok.Getter;
import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@Getter
public class CoinbaseWsAuth {

    private final String apiKey = System.getenv("API_KEY");

    private final String secret = System.getenv("API_SECRET");

    private final String passphrase = System.getenv("API_PASSPHRASE");

    public String createSignature(String timestamp) {

        try {
            String message = timestamp + "GET" + "/users/self/verify";
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(Base64.getDecoder().decode(secret), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
