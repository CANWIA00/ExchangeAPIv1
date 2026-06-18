package com.maf.exchangeapiv1.service;

import com.maf.exchangeapiv1.scheduler.FeeManagementScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;



@ExtendWith(MockitoExtension.class)
class FeeManagementSchedulerTest {

    @Mock
    private FeeManagementService feeService;

    @   InjectMocks
    private FeeManagementScheduler feeManagementScheduler;

    @BeforeEach
    void setUp() {

    }

    @Test
    @DisplayName("Refresh Fee Rates ")
    void refreshFeeRates_whenValidFeeReceived_shouldUpdateCache() {
        BigDecimal expectedFee = new BigDecimal("0.001");
        when(feeService.fetchExchangeFeeRate()).thenReturn(expectedFee);

        feeManagementScheduler.refreshFeeRates();

        BigDecimal cachedFee = feeManagementScheduler.getFeeRate();
        assertThat(cachedFee).isEqualTo(expectedFee);
    }

    @Test
    @DisplayName("RefreshFeeRates - Null")
    void refreshFeeRates_whenNullFeeReceived_shouldNotUpdateCache() {
        when(feeService.fetchExchangeFeeRate()).thenReturn(null);

        feeManagementScheduler.refreshFeeRates();


        BigDecimal cachedFee = feeManagementScheduler.getFeeRate();
        assertThat(cachedFee).isEqualTo(new BigDecimal("0.001"));
    }

    @Test
    @DisplayName("GetFeeRate - Null")
    void getFeeRate_whenCacheEmpty_shouldReturnDefaultValue() {
        BigDecimal result = feeManagementScheduler.getFeeRate();
        assertThat(result).isEqualTo(new BigDecimal("0.001"));
    }

    @Test
    @DisplayName("GetAllFees - All Fees")
    void getAllFees_shouldReturnAllFees() {
        when(feeService.fetchExchangeFeeRate()).thenReturn(new BigDecimal("0.002"));
        feeManagementScheduler.refreshFeeRates();

        Map<String, BigDecimal> allFees = feeManagementScheduler.getAllFees();

        assertThat(allFees).containsKey("TAKER_FEE");
        assertThat(allFees.get("TAKER_FEE")).isEqualTo(new BigDecimal("0.002"));
    }
}