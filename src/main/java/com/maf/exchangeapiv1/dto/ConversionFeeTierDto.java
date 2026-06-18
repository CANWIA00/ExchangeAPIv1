package com.maf.exchangeapiv1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ConversionFeeTierDto {
    @JsonProperty("from_amount")
    private String fromAmount;

    @JsonProperty("to_amount")
    private String toAmount;

    @JsonProperty("fee_rate")
    private String feeRate;


}
