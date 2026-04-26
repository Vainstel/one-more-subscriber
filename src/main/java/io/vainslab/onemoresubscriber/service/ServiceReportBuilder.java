package io.vainslab.onemoresubscriber.service;

import io.vainslab.onemoresubscriber.entity.Payment;
import io.vainslab.onemoresubscriber.entity.Subscription;
import io.vainslab.onemoresubscriber.entity.TipPayment;
import io.vainslab.onemoresubscriber.repository.PaymentRepository;
import io.vainslab.onemoresubscriber.repository.SubscriptionRepository;
import io.vainslab.onemoresubscriber.repository.TipPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ServiceReportBuilder {

    private static final int MAX_SHOWN = 15;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd.MM HH:mm");

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final TipPaymentRepository tipPaymentRepository;

    public String buildReport(io.vainslab.onemoresubscriber.entity.Service service,
                              LocalDateTime since, String periodLabel) {
        Long serviceId = service.getId();
        List<Subscription> subs = subscriptionRepository.findAllByServiceIdWithUser(serviceId);
        List<Subscription> active = subs.stream().filter(Subscription::getActive).toList();
        List<Payment> payments = paymentRepository.findAllByServiceIdSince(serviceId, since);
        List<TipPayment> tips = tipPaymentRepository.findAllByServiceIdSince(serviceId, since);

        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("\uD83D\uDCCA <b>Отчёт: ").append(service.getName()).append("</b>\n");
        sb.append("Период: ").append(periodLabel).append("\n\n");

        // Members
        sb.append("\uD83D\uDC65 Участников: <b>").append(active.size())
                .append("/").append(service.getMaxMembers()).append("</b>\n\n");

        // Payments
        sb.append("\uD83D\uDCB0 <b>Платежи за период:</b> (").append(payments.size()).append(")\n");
        if (payments.isEmpty()) {
            sb.append("  нет\n");
        } else {
            BigDecimal totalPayments = BigDecimal.ZERO;
            int shown = Math.min(payments.size(), MAX_SHOWN);
            for (int i = 0; i < shown; i++) {
                Payment p = payments.get(i);
                sb.append("  ");
                if (p.getDeleted()) {
                    sb.append("<s>").append(p.getSubscription().getUser().getDisplayName())
                            .append(" — ").append(p.getAmount()).append(" ").append(p.getCurrency())
                            .append(" (").append(p.getCreatedAt().format(DATETIME_FMT)).append(")</s> удалён\n");
                } else {
                    sb.append(p.getSubscription().getUser().getDisplayName())
                            .append(" — ").append(p.getAmount()).append(" ").append(p.getCurrency())
                            .append(" (").append(p.getCreatedAt().format(DATETIME_FMT)).append(")\n");
                }
            }
            if (payments.size() > MAX_SHOWN) {
                sb.append("  …и ещё ").append(payments.size() - MAX_SHOWN).append(" платежей\n");
            }
            for (Payment p : payments) {
                if (!p.getDeleted()) totalPayments = totalPayments.add(p.getAmount());
            }
            sb.append("  Итого: <b>").append(totalPayments).append(" ").append(service.getCurrency()).append("</b>\n");
        }

        // Tips
        sb.append("\n☕ <b>Чаевые за период:</b> (").append(tips.size()).append(")\n");
        if (tips.isEmpty()) {
            sb.append("  нет\n");
        } else {
            BigDecimal totalTips = BigDecimal.ZERO;
            int shownTips = Math.min(tips.size(), MAX_SHOWN);
            for (int i = 0; i < shownTips; i++) {
                TipPayment t = tips.get(i);
                sb.append("  ");
                if (t.getDeleted()) {
                    sb.append("<s>").append(t.getSubscription().getUser().getDisplayName())
                            .append(" — ").append(t.getAmount())
                            .append(" (").append(t.getCreatedAt().format(DATETIME_FMT)).append(")</s> удалён\n");
                } else {
                    sb.append(t.getSubscription().getUser().getDisplayName())
                            .append(" — ").append(t.getAmount())
                            .append(" (").append(t.getCreatedAt().format(DATETIME_FMT)).append(")\n");
                }
            }
            if (tips.size() > MAX_SHOWN) {
                sb.append("  …и ещё ").append(tips.size() - MAX_SHOWN).append(" чаевых\n");
            }
            for (TipPayment t : tips) {
                if (!t.getDeleted()) totalTips = totalTips.add(t.getAmount());
            }
            sb.append("  Итого: <b>").append(totalTips).append(" ").append(service.getCurrency()).append("</b>\n");
        }

        // Balances
        sb.append("\n\uD83D\uDCCB <b>Баланс участников:</b>\n");
        for (Subscription sub : active) {
            sb.append("  ").append(sub.getUser().getDisplayName());
            if (sub.getPaidUntil() != null) {
                if (sub.getPaidUntil().isAfter(LocalDateTime.now())) {
                    long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), sub.getPaidUntil().toLocalDate());
                    sb.append(" — ✅ до ").append(sub.getPaidUntil().format(DATE_FMT))
                            .append(" (").append(daysLeft).append(" дн.)");
                } else {
                    long daysOverdue = ChronoUnit.DAYS.between(sub.getPaidUntil().toLocalDate(), LocalDate.now());
                    sb.append(" — ⚠️ просрочка ").append(daysOverdue).append(" дн.");
                }
            } else {
                sb.append(" — нет платежей");
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
