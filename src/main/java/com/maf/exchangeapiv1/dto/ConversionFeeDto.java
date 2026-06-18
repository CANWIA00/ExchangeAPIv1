package com.maf.exchangeapiv1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ConversionFeeDto {
    @JsonProperty("from_currency")
    private String fromCurrency;

    @JsonProperty("to_currency")
    private String toCurrency;

    @JsonProperty("min_fee_rate")
    private String minFeeRate;

    @JsonProperty("thirty_day_net_volume")
    private String thirtyDayNetVolume;

    @JsonProperty("available_credit")
    private String availableCredit;

    @JsonProperty("fee_tiers")
    private List<ConversionFeeTierDto> feeTiers;


}
