package com.maf.exchangeapiv1.repository;

import com.maf.exchangeapiv1.dto.AccountDto;
import com.maf.exchangeapiv1.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {

    List<Account> findByUserId(String userId);


    Optional<Account> findByUserIdAndAsset(String userId, String asset);

    @Query("""
        SELECT new com.maf.exchangeapiv1.dto.AccountDto(
            a.id, 
            a.asset, 
            a.balance, 
            a.lockedBalance, 
            a.availableBalance, 
            a.createdAt, 
            a.updatedAt,
            u.id,
            u.email,
            u.name
        )
        FROM Account a
        JOIN a.user u
        WHERE a.user.id = :userId
    """)
    List<AccountDto> findAccountDTOsByUserId(@Param("userId") String userId);

    @Query("""
        SELECT new com.maf.exchangeapiv1.dto.AccountDto(
            a.id, 
            a.asset, 
            a.balance, 
            a.lockedBalance, 
            a.availableBalance, 
            a.createdAt, 
            a.updatedAt,
            u.id,
            u.email,
            u.name
        )
        FROM Account a
        JOIN a.user u
        WHERE a.user.id = :userId AND a.asset = :asset
    """)
    Optional<AccountDto> findAccountDTOByUserIdAndAsset(@Param("userId") String userId,
                                                        @Param("asset") String asset);
}
