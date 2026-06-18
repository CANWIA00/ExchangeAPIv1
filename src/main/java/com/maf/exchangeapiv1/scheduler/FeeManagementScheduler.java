package com.maf.exchangeapiv1.scheduler;

import com.maf.exchangeapiv1.service.FeeManagementService;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FeeManagementScheduler {
    private final FeeManagementService feeService;
    private final ConcurrentHashMap<String, BigDecimal> feeCache = new ConcurrentHashMap<>();//pair,fee

    public FeeManagementScheduler(FeeManagementService feeService) {
        this.feeService = feeService;
    }

    @PostConstruct
    public void init() {
        if (feeCache.isEmpty()) {
            refreshFeeRates();
        }
    }

    // 3AM
    @Scheduled(cron = "0 0 3 * * *")
    public void refreshFeeRates() {
        try {
            BigDecimal rate = feeService.fetchExchangeFeeRate();
            if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
                System.out.println("[FEE] Invalid fee, skipping cache update");
                return;
            }
            feeCache.put("TAKER_FEE", rate);
        } catch (Exception e) {
            System.err.println("[FEE ERROR] " + e.getMessage());
        }
    }

    public Map<String, BigDecimal> getAllFees() {
        return feeCache;
    }

    public BigDecimal getFeeRate() {
        return feeCache.getOrDefault("GLOBAL_RATE", new BigDecimal("0.001"));
    }


}
