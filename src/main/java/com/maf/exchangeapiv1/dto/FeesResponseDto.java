package com.maf.exchangeapiv1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FeesResponseDto {

    @JsonProperty("maker_fee_rate")
    private String makerFeeRate;

    @JsonProperty("taker_fee_rate")
    private String takerFeeRate;

    @JsonProperty("usd_volume")
    private String usdVolume;
}