package com.maf.exchangeapiv1.thirdPartAPIGateWay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maf.exchangeapiv1.dto.AccountDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;



@Slf4j
@Service
public class BinanceService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String BASE_URL = "https://testnet.binance.vision/api/v3";

    private final String API_KEY;
    private final String SECRET_KEY;
    private boolean isConnected = false;

    public BinanceService() {
        this.API_KEY = System.getenv("API_KEY_BINANCE");
        this.SECRET_KEY = System.getenv("API_SECRET_BINANCE");
        if (API_KEY == null || API_KEY.isEmpty() || SECRET_KEY == null || SECRET_KEY.isEmpty()) {
            log.warn("[BINANCE] API keys not found!");
            return;
        }
        log.info("[BINANCE] Initializing with API Key: {}", maskApiKey(API_KEY));
        this.isConnected = testConnection();
        if (isConnected) {
            log.info("[BINANCE] Connected to Binance Testnet successfully!");
        } else {
            log.warn("[BINANCE] Could not connect to Binance Testnet.");
        }
    }

    private boolean testConnection() {
        try {
            String url = BASE_URL + "/ping";
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-MBX-APIKEY", API_KEY);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.error("[BINANCE] Connection test error: {}", e.getMessage());
            return false;
        }
    }

    public List<AccountDto> getAccountList(String userId) {
        if (!isConnected) {
            log.warn("[BINANCE] Not connected, returning empty list");
            return Collections.emptyList();
        }
        try {
            String accountInfoJson = getAccountInfo();
            JsonNode accountJson = objectMapper.readTree(accountInfoJson);
            JsonNode balances = accountJson.get("balances");
            List<AccountDto> accounts = new ArrayList<>();
            String username = "Binance User";

            for (JsonNode balanceNode : balances) {
                String asset = balanceNode.get("asset").asText();
                BigDecimal free = new BigDecimal(balanceNode.get("free").asText());
                BigDecimal locked = new BigDecimal(balanceNode.get("locked").asText());
                BigDecimal total = free.add(locked);

                AccountDto account = AccountDto.builder()
                        .id(UUID.randomUUID().toString())
                        .asset(asset)
                        .balance(total)
                        .availableBalance(free)
                        .lockedBalance(locked)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .userId(userId)
                        .userEmail(username)
                        .userName(username)
                        .build();

                accounts.add(account);
            }

            log.info("[BINANCE] Fetched {} assets from Binance", accounts.size());
            return accounts;

        } catch (Exception e) {
            log.error("[BINANCE] Failed to fetch account info: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String getAccountInfo() throws Exception {
        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        String recvWindow = "5000";
        String queryString = "timestamp=" + timestamp + "&recvWindow=" + recvWindow;
        String signature = generateSignature(queryString, SECRET_KEY);
        String url = BASE_URL + "/account?" + queryString + "&signature=" + signature;
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-MBX-APIKEY", API_KEY);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
        );

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Failed to get account info: " + response.getStatusCode());
        }

        return response.getBody();
    }

    private String generateSignature(String data, String secretKey) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                secretKey.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        sha256_HMAC.init(secretKeySpec);
        byte[] hash = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
