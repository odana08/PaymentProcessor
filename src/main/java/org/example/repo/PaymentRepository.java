package org.example.repo;

import org.example.model.Currency;
import org.example.model.Payment;
import org.example.model.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    @EntityGraph(attributePaths = "notificationChannels")
    Optional<Payment> findByReference(UUID reference);

    boolean existsByReference(UUID reference);

    List<Payment> findByStatus(PaymentStatus status);

    List<Payment> findByCurrency(Currency currency);

    @Transactional
    long deleteByReference(UUID reference);

    @Query("""
            select p
            from Payment p
            where (:status is null or p.status = :status)
              and (:currency is null or p.currency = :currency)
              and (:paymentType is null or lower(p.type.name) = lower(:paymentType))
            """)
    Page<Payment> search(
            @Param("status") PaymentStatus status,
            @Param("currency") Currency currency,
            @Param("paymentType") String paymentType,
            Pageable pageable
    );
}
