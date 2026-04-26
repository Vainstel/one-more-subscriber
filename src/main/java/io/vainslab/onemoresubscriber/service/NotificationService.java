package io.vainslab.onemoresubscriber.service;

import io.vainslab.onemoresubscriber.config.BotProperties;
import io.vainslab.onemoresubscriber.entity.NotificationLog;
import io.vainslab.onemoresubscriber.entity.Subscription;
import io.vainslab.onemoresubscriber.repository.NotificationLogRepository;
import io.vainslab.onemoresubscriber.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final String TYPE_PAYMENT_DUE = "PAYMENT_DUE";
    private static final String TYPE_EXPIRING_SOON = "EXPIRING_SOON";
    private static final int OVERDUE_REPEAT_DAYS = 7;

    private final SubscriptionRepository subscriptionRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final BotProperties botProperties;

    public List<Subscription> findOverdueSubscriptions() {
        return subscriptionRepository.findOverdueWithDetails(LocalDateTime.now());
    }

    public List<Subscription> findExpiringSoon() {
        int days = botProperties.getNotification().getReminderIntervalDays();
        LocalDateTime now = LocalDateTime.now();
        return subscriptionRepository.findExpiringSoonWithDetails(now, now.plusDays(days));
    }

    public boolean shouldNotifyOverdue(Subscription subscription) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(OVERDUE_REPEAT_DAYS);
        return !notificationLogRepository.existsBySubscriptionIdAndTypeAndSentAtAfter(
                subscription.getId(), TYPE_PAYMENT_DUE, cutoff);
    }

    public boolean shouldNotifyExpiring(Subscription subscription) {
        int days = botProperties.getNotification().getReminderIntervalDays();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        return !notificationLogRepository.existsBySubscriptionIdAndTypeAndSentAtAfter(
                subscription.getId(), TYPE_EXPIRING_SOON, cutoff);
    }

    @Transactional
    public void markNotifiedOverdue(Subscription subscription) {
        notificationLogRepository.save(new NotificationLog(subscription, TYPE_PAYMENT_DUE));
    }

    @Transactional
    public void markNotifiedExpiring(Subscription subscription) {
        notificationLogRepository.save(new NotificationLog(subscription, TYPE_EXPIRING_SOON));
    }
}
