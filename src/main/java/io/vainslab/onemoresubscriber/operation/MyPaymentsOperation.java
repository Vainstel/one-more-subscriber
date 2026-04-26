package io.vainslab.onemoresubscriber.operation;

import io.vainslab.onemoresubscriber.bot.KeyboardBuilder;
import io.vainslab.onemoresubscriber.entity.BalanceDeduction;
import io.vainslab.onemoresubscriber.entity.Payment;
import io.vainslab.onemoresubscriber.entity.Subscription;
import io.vainslab.onemoresubscriber.entity.TipPayment;
import io.vainslab.onemoresubscriber.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MyPaymentsOperation implements ServiceOperation {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final int PAGE_SIZE = 10;

    private final PaymentService paymentService;

    @Override
    public String getCode() { return "mypay"; }

    @Override
    public int getOrder() { return 4; }

    @Override
    public String getButtonLabel() {
        return "\uD83D\uDCCB Мои платежи";
    }

    @Override
    public boolean isAvailable(Subscription subscription) {
        return subscription != null && subscription.getActive();
    }

    @Override
    public void execute(OperationContext ctx) {
        Long subId = ctx.getSubscription().getId();
        String currency = ctx.getService().getCurrency();

        List<Payment> payments = paymentService.getPayments(subId);
        List<TipPayment> tips = paymentService.getTips(subId);
        List<BalanceDeduction> deductions = paymentService.getDeductions(subId);

        StringBuilder sb = new StringBuilder();

        // Пополнения
        sb.append("<b>\uD83D\uDCB0 Пополнения:</b>\n\n");
        if (payments.isEmpty()) {
            sb.append("  пока нет\n");
        } else {
            int shown = Math.min(payments.size(), PAGE_SIZE);
            for (int i = 0; i < shown; i++) {
                Payment p = payments.get(i);
                sb.append("• ").append(p.getAmount()).append(" ").append(p.getCurrency())
                        .append(" — ").append(p.getCreatedAt().format(FMT)).append("\n");
            }
            if (payments.size() > PAGE_SIZE) {
                sb.append("…и ещё ").append(payments.size() - PAGE_SIZE).append("\n");
            }
        }

        // Чаевые (скрыта если пустая)
        if (!tips.isEmpty()) {
            sb.append("\n<b>☕ Чаевые:</b>\n\n");
            int shown = Math.min(tips.size(), PAGE_SIZE);
            for (int i = 0; i < shown; i++) {
                TipPayment t = tips.get(i);
                sb.append("• ").append(t.getAmount()).append(" ").append(currency)
                        .append(" — ").append(t.getCreatedAt().format(FMT)).append("\n");
            }
            if (tips.size() > PAGE_SIZE) {
                sb.append("…и ещё ").append(tips.size() - PAGE_SIZE).append("\n");
            }
        }

        // Списания
        int maxMembers = ctx.getService().getMaxMembers();
        var monthlyCost = ctx.getService().getMonthlyCost();
        sb.append("\n<b>\uD83D\uDCC9 Списания:</b>\n");
        sb.append("<i>сумма — дата участники/макс/мес</i>\n\n");
        if (deductions.isEmpty()) {
            sb.append("  пока нет\n");
        } else {
            int shown = Math.min(deductions.size(), PAGE_SIZE);
            for (int i = 0; i < shown; i++) {
                BalanceDeduction d = deductions.get(i);
                sb.append("• ").append(d.getAmount()).append(" — ")
                        .append(d.getCreatedAt().format(FMT)).append(" ")
                        .append(d.getMemberCount()).append("/").append(maxMembers)
                        .append("/").append(monthlyCost)
                        .append("\n");
            }
            if (deductions.size() > PAGE_SIZE) {
                sb.append("…и ещё ").append(deductions.size() - PAGE_SIZE).append("\n");
            }
        }

        ctx.reply(sb.toString(),
                KeyboardBuilder.backToServiceKeyboard(ctx.getService().getId()));
    }
}
