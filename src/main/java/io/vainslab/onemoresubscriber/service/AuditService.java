package io.vainslab.onemoresubscriber.service;

import io.vainslab.onemoresubscriber.entity.AuditLog;
import io.vainslab.onemoresubscriber.entity.BotUser;
import io.vainslab.onemoresubscriber.repository.AuditLogRepository;
import org.springframework.data.domain.PageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logJoin(BotUser user, String serviceName) {
        save(user, "Присоединился к подписке " + serviceName);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLeave(BotUser user, String serviceName) {
        save(user, "Покинул подписку " + serviceName);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logKick(BotUser admin, BotUser target, String serviceName) {
        save(admin, "Исключил " + target.getDisplayName() + " из подписки " + serviceName);
        save(target, "Исключён из подписки " + serviceName + " админом " + admin.getDisplayName());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logPayment(BotUser user, String serviceName, BigDecimal amount, String currency) {
        save(user, "Добавил платёж " + amount + " " + currency + " в " + serviceName);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logPaymentDelete(BotUser user, String serviceName, BigDecimal amount, String currency,
                                  java.time.LocalDateTime paymentDate) {
        save(user, "Удалил платёж от " + paymentDate.format(FMT) + " на сумму " + amount + " " + currency
                + " в " + serviceName);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logTip(BotUser user, String serviceName, BigDecimal amount, String currency) {
        save(user, "Оставил чаевые " + amount + " " + currency + " в " + serviceName);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logCreatorMessage(BotUser user, String serviceName, String messageText) {
        String preview = messageText.length() > 100 ? messageText.substring(0, 100) + "…" : messageText;
        save(user, "Написал создателю " + serviceName + ": «" + preview + "»");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logBillingToggle(BotUser admin, String serviceName, boolean billingActive) {
        String action = billingActive
                ? "Возобновил списание для " + serviceName
                : "Приостановил списание для " + serviceName;
        save(admin, action);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logError(BotUser user, String action, Exception ex) {
        String msg = "Ошибка при " + action + ": " + ex.getClass().getSimpleName() + " — " + ex.getMessage();
        save(user, msg);
    }

    public List<AuditLog> getRecentByUser(Long userId, int limit) {
        return auditLogRepository.findAllByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit));
    }

    private void save(BotUser user, String action) {
        try {
            auditLogRepository.save(new AuditLog(user, action));
        } catch (Exception e) {
            log.error("Failed to save audit log: {}", action, e);
        }
    }
}
