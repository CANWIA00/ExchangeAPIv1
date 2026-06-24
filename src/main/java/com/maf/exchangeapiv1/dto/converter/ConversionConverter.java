package com.maf.exchangeapiv1.dto.converter;

import com.maf.exchangeapiv1.dto.ConversionDto;
import com.maf.exchangeapiv1.model.Conversion;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ConversionConverter {

    public ConversionDto toDTO(Conversion conversion) {
        if (conversion == null) {
            return null;
        }
        return ConversionDto.builder()
                .id(conversion.getId())
                .userId(conversion.getUserId())
                .fromAsset(conversion.getFromAsset())
                .toAsset(conversion.getToAsset())
                .fromAmount(conversion.getFromAmount())
                .toAmount(conversion.getToAmount())
                .rate(conversion.getRate())
                .fromAccountId(conversion.getFromAccountId())
                .toAccountId(conversion.getToAccountId())
                .transactionId(conversion.getTransactionId())
                .status(conversion.getStatus())
                .referenceId(conversion.getReferenceId())
                .description(conversion.getDescription())
                .createdAt(conversion.getCreatedAt())
                .completedAt(conversion.getCompletedAt())
                .build();
    }


    public List<ConversionDto> toDTOList(List<Conversion> conversions) {
        return conversions.stream().map(this::toDTO).collect(Collectors.toList());
    }
}
