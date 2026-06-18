package com.maf.exchangeapiv1.service;

import com.maf.exchangeapiv1.cache.OrderBookCache;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class SlippageServiceTest {

    @Mock
    private OrderBookCache orderBookCache;

    @InjectMocks
    private SlippageService slippageService;

    private static final String PAIR = "BTC-USD";
    private static final BigDecimal AMOUNT = BigDecimal.ONE;

    @Test
    void whenSlippageServiceOccursWithOrdinaryBuyOperation_calculateSlippage_shouldReturnSlippage() {

        when(orderBookCache.getBestAsk(PAIR)).thenReturn(new BigDecimal("30000"));
        when(orderBookCache.getAveragePriceWithVolume(eq(PAIR), eq(AMOUNT), eq(true)))
                .thenReturn(new BigDecimal("30035"));

        BigDecimal result = slippageService.calculateSlippage(PAIR, AMOUNT, true);

        assertThat(result).isEqualTo(new BigDecimal("0.001167"));

        verify(orderBookCache).getBestAsk(PAIR);
        verify(orderBookCache).getAveragePriceWithVolume(PAIR, AMOUNT, true);

    }

    @Test
    @DisplayName("Need to return %0.0005")
    void whenMarketSlippageIsLow_calculateSlippage_shouldReturnMinimumBuffer() {
        // %0.01 Lower Gap
        when(orderBookCache.getBestAsk(PAIR)).thenReturn(new BigDecimal("30000"));
        when(orderBookCache.getAveragePriceWithVolume(eq(PAIR), eq(AMOUNT), eq(true)))
                .thenReturn(new BigDecimal("30003"));  // Only 3 USD gap = %0.01

        BigDecimal result = slippageService.calculateSlippage(PAIR, AMOUNT, true);
        assertThat(result).isEqualTo(new BigDecimal("0.0005"));

        verify(orderBookCache).getBestAsk(PAIR);
        verify(orderBookCache).getAveragePriceWithVolume(PAIR, AMOUNT, true);
    }

    @Test
    @DisplayName("Need to return %0.05")
    void whenMarketSlippageIsHigh_shouldReturnMaxLimit() {

        when(orderBookCache.getBestAsk(PAIR)).thenReturn(new BigDecimal("30000"));
        when(orderBookCache.getAveragePriceWithVolume(eq(PAIR), eq(AMOUNT), eq(true)))
                .thenReturn(new BigDecimal("33000"));

        BigDecimal result = slippageService.calculateSlippage(PAIR, AMOUNT, true);
        assertThat(result).isEqualTo(new BigDecimal("0.05"));
    }

}