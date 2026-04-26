package io.vainslab.onemoresubscriber.handler;

import io.vainslab.onemoresubscriber.entity.Service;
import io.vainslab.onemoresubscriber.entity.Subscription;
import io.vainslab.onemoresubscriber.operation.ServiceOperation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

public abstract class AbstractServiceHandler implements ServiceHandler {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final List<ServiceOperation> defaultOperations;

    protected AbstractServiceHandler(List<ServiceOperation> defaultOperations) {
        this.defaultOperations = defaultOperations;
    }

    @Override
    public List<ServiceOperation> getDefaultOperations() {
        return defaultOperations;
    }

    @Override
    public List<ServiceOperation> getCustomOperations() {
        return List.of();
    }

    @Override
    public String buildServiceInfoMessage(Service service, Subscription subscription, int memberCount, BigDecimal userBalance) {
        BigDecimal perMember = memberCount > 0
                ? service.getMonthlyCost().divide(BigDecimal.valueOf(memberCount), 2, RoundingMode.HALF_UP)
                : service.getMonthlyCost();

        StringBuilder sb = new StringBuilder();
        sb.append("<b>").append(service.getName()).append("</b>\n\n");

        if (service.getDescription() != null) {
            sb.append(service.getDescription()).append("\n\n");
        }

        BigDecimal dailyRate = perMember.divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);

        if (service.getBillingActive()) {
            sb.append("🟢 Подписка активна\n");
        } else {
            sb.append("🔴 Подписка приостановлена\n");
        }

        sb.append("👥 Участников: ").append(memberCount).append("/").append(service.getMaxMembers()).append("\n");

        boolean isMember = subscription != null && subscription.getActive();
        if (isMember) {
            sb.append("👤 С каждого: ").append(perMember).append(" ").append(service.getCurrency()).append("/мес\n");
        } else {
            BigDecimal perMemberIfJoin = service.getMonthlyCost()
                    .divide(BigDecimal.valueOf(memberCount + 1), 2, RoundingMode.HALF_UP);
            sb.append("👤 При вступлении: ").append(perMemberIfJoin).append(" ").append(service.getCurrency()).append("/мес\n");
        }

        if (subscription != null && subscription.getActive() && userBalance != null) {
            sb.append("\n💰 Баланс: <b>").append(userBalance).append(" ").append(service.getCurrency()).append("</b>\n");
            sb.append("📉 Списывается: ").append(dailyRate).append(" ").append(service.getCurrency()).append("/день\n");
        }

        if (service.getRules() != null && !service.getRules().isBlank()) {
            sb.append("\n📋 <b>Правила:</b>\n").append(service.getRules()).append("\n");
        }

        boolean isNotMember = subscription == null || !subscription.getActive();
        if (isNotMember && service.getJoinDescription() != null && !service.getJoinDescription().isBlank()) {
            sb.append("\nℹ️ ").append(service.getJoinDescription()).append("\n");
        }

        sb.append("\n");

        if (isNotMember) {
            sb.append("Вы не являетесь участником этого сервиса.");
        } else if (subscription.isInGracePeriod() && !subscription.getPaidUntil().isAfter(subscription.getJoinedAt())) {
            sb.append("🆕 Вы только что присоединились! Внесите первый платёж в течении 1 часа.");
        } else if (subscription.getPaidUntil() == null) {
            sb.append("⏳ Оплата пока не вносилась.");
        } else if (subscription.getPaidUntil().isAfter(LocalDateTime.now())) {
            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), subscription.getPaidUntil().toLocalDate());
            sb.append("✅ Оплачено до: <b>").append(subscription.getPaidUntil().format(FMT)).append("</b>\n");
            sb.append("📅 Осталось дней: <b>").append(daysLeft).append("</b> (не считая сегодня)\n\n");
            sb.append("Вы отличный участник! Всё оплачено, пользуйтесь на здоровье 🎉");
        } else {
            long daysOverdue = ChronoUnit.DAYS.between(subscription.getPaidUntil().toLocalDate(), LocalDate.now());
            sb.append("⚠️ <b>Задолженность!</b> Оплата закончилась: ")
                    .append(subscription.getPaidUntil().format(FMT)).append("\n");
            sb.append("📅 Дней просрочки: <b>").append(daysOverdue).append("</b>");
        }

        return sb.toString();
    }
}
