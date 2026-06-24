package com.maf.exchangeapiv1.controller;

import com.maf.exchangeapiv1.dto.ConversionDto;
import com.maf.exchangeapiv1.service.ConversionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/conversions")
public class ConversionController {

    private final ConversionService conversionService;

    @PostMapping
    public ResponseEntity<ConversionDto> createConversionOffer(Authentication authentication, @RequestParam String fromAsset,
                                                               @RequestParam String toAsset, @RequestParam BigDecimal amount,
                                                               @RequestParam(required = false) String description,
                                                               @RequestParam(required = false) String side,
                                                               @RequestParam(required = false) String conversionType) {
        String userId = authentication.getName();
        ConversionDto conversion = conversionService.createConversionOffer(
                userId,
                fromAsset.toUpperCase(),
                toAsset.toUpperCase(),
                amount,
                description,
                side,
                conversionType
        );
        return ResponseEntity.ok(conversion);
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<?> acceptConversion(Authentication authentication, @PathVariable String id) {
        String userId = authentication.getName();
        try {
            ConversionDto completedConversion = conversionService.acceptConversionOffer(id, userId);
            return ResponseEntity.ok(Map.of(
                    "message", "Conversion completed successfully",
                    "conversion", completedConversion
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelConversion(Authentication authentication,
                                              @PathVariable String id) {
        String userId = authentication.getName();

        try {
            ConversionDto cancelledConversion = conversionService.cancelConversion(id, userId);
            return ResponseEntity.ok(Map.of(
                    "message", "Conversion cancelled successfully",
                    "conversion", cancelledConversion
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping
    public ResponseEntity<List<ConversionDto>> getConversions(Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(conversionService.getUserConversions(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConversionDto> getConversion(Authentication authentication, @PathVariable String id) {
        String userId = authentication.getName();
        ConversionDto conversion = conversionService.getConversion(id);

        if (!conversion.getUserId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(conversion);
    }


}
