package io.vainslab.onemoresubscriber.repository;

import io.vainslab.onemoresubscriber.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    boolean existsBySubscriptionIdAndTypeAndSentAtAfter(Long subscriptionId, String type, LocalDateTime after);
}
