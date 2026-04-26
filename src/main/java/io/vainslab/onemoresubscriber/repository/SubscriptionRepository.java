package io.vainslab.onemoresubscriber.repository;

import io.vainslab.onemoresubscriber.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.Modifying;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByUserIdAndServiceId(Long userId, Long serviceId);

    @Query("SELECT s FROM Subscription s JOIN FETCH s.user JOIN FETCH s.service svc JOIN FETCH svc.createdBy WHERE s.user.id = :userId AND s.service.id = :serviceId")
    Optional<Subscription> findByUserAndServiceWithDetails(@Param("userId") Long userId, @Param("serviceId") Long serviceId);

    @Query("SELECT s FROM Subscription s JOIN FETCH s.user JOIN FETCH s.service svc JOIN FETCH svc.createdBy WHERE s.id = :id")
    Optional<Subscription> findByIdWithDetails(@Param("id") Long id);

    List<Subscription> findAllByServiceIdAndActiveTrue(Long serviceId);

    @Query("SELECT s FROM Subscription s JOIN FETCH s.service WHERE s.service.id = :serviceId AND s.active = true")
    List<Subscription> findAllActiveByServiceIdWithService(@Param("serviceId") Long serviceId);

    int countByServiceIdAndActiveTrue(Long serviceId);

    @Query("SELECT s FROM Subscription s JOIN FETCH s.user JOIN FETCH s.service svc WHERE s.active = true AND svc.billingActive = true AND s.paidUntil < :dateTime")
    List<Subscription> findOverdueWithDetails(@Param("dateTime") LocalDateTime dateTime);

    @Query("SELECT s FROM Subscription s JOIN FETCH s.user JOIN FETCH s.service svc WHERE s.active = true AND svc.billingActive = true AND s.paidUntil > :from AND s.paidUntil <= :to")
    List<Subscription> findExpiringSoonWithDetails(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT s FROM Subscription s JOIN FETCH s.service svc WHERE s.active = true AND svc.billingActive = true")
    List<Subscription> findAllActiveForDeduction();

    @Query("SELECT s FROM Subscription s JOIN FETCH s.user WHERE s.service.id = :serviceId")
    List<Subscription> findAllByServiceIdWithUser(@Param("serviceId") Long serviceId);

    @Modifying
    @Query("UPDATE Subscription s SET s.lastDeductedAt = :now WHERE s.service.id = :serviceId AND s.active = true")
    void resetLastDeductedAt(@Param("serviceId") Long serviceId, @Param("now") LocalDateTime now);
}
