package com.maf.exchangeapiv1.service;

import com.maf.exchangeapiv1.dto.FeesResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeeManagementServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private FeeManagementService feeManagementService;

    @Test
    @DisplayName("fetchExchangeFeeRate")
    void fetchExchangeFeeRate_whenSuccess_shouldReturnFee() {
        FeesResponseDto mockResponse = new FeesResponseDto();
        mockResponse.setMakerFeeRate("0.001");
        mockResponse.setTakerFeeRate("0.001");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/fees")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(FeesResponseDto.class)).thenReturn(Mono.just(mockResponse));

        BigDecimal result = feeManagementService.fetchExchangeFeeRate();

        assertThat(result).isEqualTo(new BigDecimal("0.001"));

        verify(webClient).get();
        verify(requestHeadersUriSpec).uri("/fees");
        verify(requestHeadersSpec).retrieve();
        verify(responseSpec).bodyToMono(FeesResponseDto.class);
    }

    @Test
    @DisplayName("fetchExchangeFeeRate - Exception Null ")
    void fetchExchangeFeeRate_whenApiError_shouldReturnNull() {
        // GIVEN
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/fees")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(FeesResponseDto.class)).thenReturn(Mono.error(new RuntimeException("API Error")));

        BigDecimal result = feeManagementService.fetchExchangeFeeRate();

        assertThat(result).isNull();

        verify(webClient).get();
        verify(requestHeadersUriSpec).uri("/fees");
        verify(requestHeadersSpec).retrieve();
    }



}