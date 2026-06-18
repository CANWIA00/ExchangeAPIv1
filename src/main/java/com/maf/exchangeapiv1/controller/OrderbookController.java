package com.maf.exchangeapiv1.controller;

import com.maf.exchangeapiv1.exception.InsufficientLiquidityException;
import com.maf.exchangeapiv1.service.OrderbookService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.Map;


@RestController
@RequestMapping("/api/exchange")
public class OrderbookController {

    private final OrderbookService orderbookService;

    public OrderbookController(OrderbookService orderbookService) {
        this.orderbookService = orderbookService;
    }

    // All prices for stock market by streaming
    @GetMapping(value = "/stream/prices", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Map<String, BigDecimal>> streamPrices() {
        return orderbookService.streamPrices();
    }

    // All prices for stock market with ticker channel
    @GetMapping("/prices")
    public ResponseEntity<?> streamAllPricesTicker() {
        return ResponseEntity.ok(orderbookService.getAllPricesTicker());
    }

    // The price for selected coin from stock market with ticker channel (ex: /api/exchange/price?pair=BTC-USD)
    @GetMapping("/price")
    public ResponseEntity<?> getSelectedPriceTicker(@RequestParam String pair) {
        return ResponseEntity.ok(orderbookService.getSelectedPriceTicker(pair.toUpperCase()));
    }

    // The best price for selected coin from SM with level-2 channel
    @GetMapping("/orderbook/best-price")
    public ResponseEntity<Map<String, BigDecimal>> getBestPrice(
            @RequestParam String pair) {
        return ResponseEntity.ok(orderbookService.getBestPrice(pair.toUpperCase()));
    }

    //The best price for selected coin for selected side and amount considering volume from level-2 channel
    @GetMapping("/orderbook/volume/best-price")
    public ResponseEntity<BigDecimal> getBestPriceWithVolume(
            @RequestParam String pair,
            @RequestParam BigDecimal amount,
            @RequestParam String side) {
        return ResponseEntity.ok(orderbookService.getBestPriceWithVolume(pair, amount, side));
    }

    // Total and average price for selected coin for selected side and amount considering volume from level-2 channel
    @GetMapping("/orderbook/totalPrice")
    public ResponseEntity<Map<String, BigDecimal>> getOrderTotalPrice(
            @RequestParam String pair,
            @RequestParam BigDecimal amount,
            @RequestParam String side) {
        return ResponseEntity.ok(orderbookService.getOrderTotalPrice(pair, side, amount));
    }

    @ExceptionHandler(InsufficientLiquidityException.class)
    public ResponseEntity<Map<String, String>> handleInsufficientLiquidity(InsufficientLiquidityException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

}
