package io.vainslab.onemoresubscriber.operation;

import io.vainslab.onemoresubscriber.bot.KeyboardBuilder;
import io.vainslab.onemoresubscriber.bot.UserSession;
import io.vainslab.onemoresubscriber.bot.UserSession.UserState;
import io.vainslab.onemoresubscriber.entity.Subscription;
import io.vainslab.onemoresubscriber.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class TipOperation implements ServiceOperation {

    private final PaymentService paymentService;

    @Override
    public String getCode() { return "tip"; }

    @Override
    public int getOrder() { return 6; }

    @Override
    public String getButtonLabel() {
        return "✝️ Чаевые создателю";
    }

    @Override
    public boolean isAvailable(Subscription subscription) {
        return false; // temporarily disabled
    }

    @Override
    public void execute(OperationContext ctx) {
        BigDecimal available = ctx.getSubscription().getBalance().max(BigDecimal.ZERO);
        String currency = ctx.getService().getCurrency();

        StringBuilder sb = new StringBuilder();
        sb.append("✝️ Чаевые — способ поблагодарить создателя.\n");
        sb.append("Сумма будет списана из вашего баланса.\n\n");

        if (available.compareTo(BigDecimal.ZERO) <= 0) {
            sb.append("❌ Ваш баланс: <b>0 ").append(currency).append("</b>\n");
            sb.append("Сейчас оставить чаевые нельзя — сначала внесите платёж.");
            ctx.reply(sb.toString(), KeyboardBuilder.backToServiceKeyboard(ctx.getService().getId()));
            return;
        }

        sb.append("💰 Доступно для чаевых: <b>").append(available).append(" ").append(currency).append("</b>\n\n");
        sb.append("⚠️ <b>Осторожно:</b> удалить чаевые из системы нельзя!\n\n");
        sb.append("Введите сумму чаевых:");

        UserSession session = ctx.getSessionManager().getSession(ctx.getBotUser().getTelegramId());
        session.setState(UserState.AWAITING_TIP_AMOUNT);
        session.setServiceId(ctx.getService().getId());
        session.setSubscriptionId(ctx.getSubscription().getId());

        ctx.reply(sb.toString(), KeyboardBuilder.backToServiceKeyboard(ctx.getService().getId()));
    }
}
