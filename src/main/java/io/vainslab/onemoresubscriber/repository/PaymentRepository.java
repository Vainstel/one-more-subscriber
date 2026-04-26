package io.vainslab.onemoresubscriber.repository;

import io.vainslab.onemoresubscriber.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("SELECT p FROM Payment p JOIN FETCH p.subscription s JOIN FETCH s.service WHERE p.id = :id")
    Optional<Payment> findByIdWithDetails(@Param("id") Long id);

    List<Payment> findAllBySubscriptionIdAndDeletedFalseOrderByCreatedAtDesc(Long subscriptionId);

    List<Payment> findAllBySubscriptionIdAndDeletedFalseAndCreatedAtAfterOrderByCreatedAtDesc(
            Long subscriptionId, LocalDateTime after);

    @Query("SELECT p FROM Payment p JOIN FETCH p.subscription s JOIN FETCH s.user WHERE s.service.id = :serviceId AND p.createdAt >= :since ORDER BY p.createdAt DESC")
    List<Payment> findAllByServiceIdSince(@Param("serviceId") Long serviceId, @Param("since") LocalDateTime since);

}
