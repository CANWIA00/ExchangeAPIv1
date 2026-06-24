package com.maf.exchangeapiv1.service;


import com.maf.exchangeapiv1.cache.OrderBookCache;
import com.maf.exchangeapiv1.dto.FinalPriceDto;
import com.maf.exchangeapiv1.scheduler.FeeManagementScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpreadEngineServiceTest {

    @Mock
    private FeeManagementScheduler scheduler;

    @Mock
    private OrderBookCache orderBookCache;

    @Mock
    private SlippageService slippageService;

    @InjectMocks
    private SpreadEngineService spreadEngineService;

    private static final String PAIR = "BTC-USD";
    private static final BigDecimal AMOUNT = BigDecimal.ONE;
    private static final BigDecimal FEE_RATE = new BigDecimal("0.001");  // %0.1


    @BeforeEach
    void setUp() {
        when(scheduler.getFeeRate()).thenReturn(FEE_RATE);
    }


    @Test
    @DisplayName("Base Buy")
    void calculateBaseFinalPrice_forBuy_shouldReturnCorrectTotalCost() {
        BigDecimal avgPrice = new BigDecimal("30035");
        BigDecimal feeRate = new BigDecimal("0.001");
        BigDecimal profitMargin = new BigDecimal("0.002");

        when(scheduler.getFeeRate()).thenReturn(feeRate);
        when(orderBookCache.getAveragePriceWithVolume(eq(PAIR), eq(AMOUNT), eq(true)))
                .thenReturn(avgPrice);

        FinalPriceDto result = spreadEngineService.calculateBaseFinalPrice(AMOUNT, PAIR, "buy");

        BigDecimal expected = avgPrice
                .multiply(BigDecimal.ONE.add(feeRate))
                .multiply(BigDecimal.ONE.add(profitMargin))
                .multiply(AMOUNT)
                .setScale(2, RoundingMode.HALF_UP);
        assertThat(result.getTotalCost()).isEqualTo(expected);
    }

    @Test
    @DisplayName("Base Sell")
    void calculateBaseFinalPrice_forSell_shouldReturnCorrectTotalCost() {
        BigDecimal avgPrice = new BigDecimal("29965");
        BigDecimal feeRate = new BigDecimal("0.001");
        BigDecimal profitMargin = new BigDecimal("0.002");

        when(scheduler.getFeeRate()).thenReturn(feeRate);
        when(orderBookCache.getAveragePriceWithVolume(eq(PAIR), eq(AMOUNT), eq(false)))
                .thenReturn(avgPrice);

        FinalPriceDto result = spreadEngineService.calculateBaseFinalPrice(AMOUNT, PAIR, "sell");

        BigDecimal expected = avgPrice
                .multiply(BigDecimal.ONE.subtract(feeRate))
                .multiply(BigDecimal.ONE.subtract(profitMargin))
                .multiply(AMOUNT)
                .setScale(2, RoundingMode.HALF_UP);

        assertThat(result.getTotalCost()).isEqualTo(expected);
    }

    @Test
    @DisplayName("Quote Buy")
    void calculateQuoteFinalPrice_forBuy_shouldReturnCorrectBTCAmount() {
        BigDecimal targetAmount = new BigDecimal("500");
        BigDecimal bestPrice = new BigDecimal("30000");
        BigDecimal feeRate = new BigDecimal("0.001");
        BigDecimal profitMargin = new BigDecimal("0.002");
        BigDecimal slippage = new BigDecimal("0.0005");

        when(scheduler.getFeeRate()).thenReturn(feeRate);
        when(orderBookCache.getBestAsk(PAIR)).thenReturn(bestPrice);
        when(slippageService.calculateSlippage(eq(PAIR), any(BigDecimal.class), eq(true)))
                .thenReturn(slippage);

        FinalPriceDto result = spreadEngineService.calculateQuoteFinalPrice(targetAmount, PAIR, "buy");

        // multiplier = (1+fee) × (1+kar) × (1+slippage)
        BigDecimal multiplier = BigDecimal.ONE.add(feeRate)
                .multiply(BigDecimal.ONE.add(profitMargin))
                .multiply(BigDecimal.ONE.add(slippage));

        BigDecimal expected = targetAmount.divide(bestPrice.multiply(multiplier), 8, RoundingMode.HALF_DOWN);
        assertThat(result.getTotalCost()).isEqualTo(expected);
    }

    @Test
    @DisplayName("Quote Sell")
    void calculateQuoteFinalPrice_forSell_shouldReturnCorrectBTCAmount() {
        BigDecimal targetAmount = new BigDecimal("500");
        BigDecimal bestPrice = new BigDecimal("30000");
        BigDecimal feeRate = new BigDecimal("0.001");
        BigDecimal profitMargin = new BigDecimal("0.002");
        BigDecimal slippage = new BigDecimal("0.0005");

        when(scheduler.getFeeRate()).thenReturn(feeRate);
        when(orderBookCache.getBestBid(PAIR)).thenReturn(bestPrice);
        when(slippageService.calculateSlippage(eq(PAIR), any(BigDecimal.class), eq(false)))
                .thenReturn(slippage);

        FinalPriceDto result = spreadEngineService.calculateQuoteFinalPrice(targetAmount, PAIR, "sell");

        BigDecimal multiplier = BigDecimal.ONE.subtract(feeRate)
                .multiply(BigDecimal.ONE.subtract(profitMargin))
                .multiply(BigDecimal.ONE.subtract(slippage));

        BigDecimal expected = targetAmount.divide(bestPrice.multiply(multiplier), 8, RoundingMode.HALF_UP);
        assertThat(result.getTotalCost()).isEqualTo(expected);
    }
}