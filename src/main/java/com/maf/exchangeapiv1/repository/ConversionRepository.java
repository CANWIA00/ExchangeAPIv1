package com.maf.exchangeapiv1.repository;

import com.maf.exchangeapiv1.model.Conversion;
import com.maf.exchangeapiv1.model.ConversionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversionRepository extends JpaRepository<Conversion, String> {


    List<Conversion> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Conversion> findByUserIdAndStatus(String userId, ConversionStatus status);

    List<Conversion> findByStatus(ConversionStatus status);

    List<Conversion> findByUserIdAndFromAsset(String userId, String fromAsset);

    List<Conversion> findByUserIdAndToAsset(String userId, String toAsset);

    Optional<Conversion> findByTransactionId(String transactionId);
}
