package com.maf.exchangeapiv1.controller;

import com.maf.exchangeapiv1.dto.FinalPriceDto;
import com.maf.exchangeapiv1.scheduler.FeeManagementScheduler;
import com.maf.exchangeapiv1.service.SpreadEngineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/management")
public class FeeManagementController {

    private final FeeManagementScheduler scheduler;
    private final SpreadEngineService spreadEngine;

    public FeeManagementController(FeeManagementScheduler scheduler, SpreadEngineService spreadEngine) {
        this.scheduler = scheduler;
        this.spreadEngine = spreadEngine;
    }

    @GetMapping("/fees")
    public Map<String, BigDecimal> getCurrentFees() {
        return scheduler.getAllFees();
    }

    @PostMapping("/refresh")
    public String manualRefresh() {
        scheduler.refreshFeeRates();
        return "Manual refresh completed.";
    }

    @GetMapping("/calculate")
    public ResponseEntity<FinalPriceDto> testCalculationBase(@RequestParam BigDecimal amount, @RequestParam String pair, @RequestParam String side) {
        return ResponseEntity.ok( spreadEngine.calculateBaseFinalPrice(amount, pair, side));
    }

    @GetMapping("/calculate/quote")
    public ResponseEntity<FinalPriceDto> testCalculationQuote(@RequestParam BigDecimal targetAmount, @RequestParam String pair, @RequestParam String side) {
        return ResponseEntity.ok(spreadEngine.calculateQuoteFinalPrice(targetAmount, pair, side));
    }
}
