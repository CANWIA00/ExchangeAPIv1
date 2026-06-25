package com.maf.exchangeapiv1.repository;

import com.maf.exchangeapiv1.model.Conversion;
import com.maf.exchangeapiv1.model.ConversionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface ConversionRepository extends JpaRepository<Conversion, String> {

    List<Conversion> findByStatus(ConversionStatus status);

    Optional<Conversion> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);
}
