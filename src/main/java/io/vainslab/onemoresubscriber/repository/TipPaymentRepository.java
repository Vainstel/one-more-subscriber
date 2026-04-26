package io.vainslab.onemoresubscriber.repository;

import io.vainslab.onemoresubscriber.entity.TipPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TipPaymentRepository extends JpaRepository<TipPayment, Long> {

    @Query("SELECT t FROM TipPayment t JOIN FETCH t.subscription s JOIN FETCH s.service WHERE t.id = :id")
    Optional<TipPayment> findByIdWithDetails(@Param("id") Long id);

    List<TipPayment> findAllBySubscriptionIdAndDeletedFalseAndCreatedAtAfterOrderByCreatedAtDesc(
            Long subscriptionId, LocalDateTime after);

    List<TipPayment> findAllBySubscriptionIdAndDeletedFalseOrderByCreatedAtDesc(Long subscriptionId);

    @Query("SELECT t FROM TipPayment t JOIN FETCH t.subscription s JOIN FETCH s.user WHERE s.service.id = :serviceId AND t.createdAt >= :since ORDER BY t.createdAt DESC")
    List<TipPayment> findAllByServiceIdSince(@Param("serviceId") Long serviceId, @Param("since") LocalDateTime since);
}
