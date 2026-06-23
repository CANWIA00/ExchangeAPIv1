package com.maf.exchangeapiv1.dto.converter;

import com.maf.exchangeapiv1.dto.AccountDto;
import com.maf.exchangeapiv1.model.Account;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AccountConverter {

    public AccountDto toDTO(Account account) {
        if (account == null) {
            return null;
        }

        return AccountDto.builder()
                .id(account.getId())
                .asset(account.getAsset())
                .balance(account.getBalance())
                .lockedBalance(account.getLockedBalance())
                .availableBalance(account.getAvailableBalance())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .userId(account.getUser() != null ? account.getUser().getId() : null)
                .userEmail(account.getUser() != null ? account.getUser().getEmail() : null)
                .userName(account.getUser() != null ? account.getUser().getName() : null)
                .build();
    }

    public List<AccountDto> toDTOList(List<Account> accounts) {
        if (accounts == null) {
            return List.of();
        }
        return accounts.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}
