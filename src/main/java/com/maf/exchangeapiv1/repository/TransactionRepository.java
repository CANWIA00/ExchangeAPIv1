package com.maf.exchangeapiv1.repository;


import com.maf.exchangeapiv1.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {


    List<Transaction> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Transaction> findByUserIdAndAssetOrderByCreatedAtDesc(String userId, String asset);

    List<Transaction> findByAccountIdOrderByCreatedAtDesc(String accountId);

    List<Transaction> findByUserIdAndTypeOrderByCreatedAtDesc(String userId, String type);

    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId AND t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<Transaction> findByUserIdAndDateRange(@Param("userId") String userId,
                                               @Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.userId = :userId AND t.type = 'TRADE' AND t.amount < 0")
    BigDecimal getTotalSpending(@Param("userId") String userId);
}
