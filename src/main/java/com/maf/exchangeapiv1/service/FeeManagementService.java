package com.maf.exchangeapiv1.service;


import com.maf.exchangeapiv1.dto.FeesResponseDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
public class FeeManagementService {

    private final WebClient webClient;

    public FeeManagementService(@Qualifier("privateWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public BigDecimal fetchExchangeFeeRate() {

        return webClient.get()
                .uri("/fees")
                .retrieve()
                .bodyToMono(FeesResponseDto.class)
                .map(res -> {
                    if (res.getMakerFeeRate() == null ||
                            res.getMakerFeeRate().isBlank()) {
                        return null;
                    }
                    return new BigDecimal(res.getTakerFeeRate());
                })
                .onErrorResume(e -> {
                    System.err.println("[FEE API ERROR] " + e.getMessage());
                    return Mono.empty();
                })
                .block();
    }
}
