package io.vainslab.onemoresubscriber.operation;

import io.vainslab.onemoresubscriber.bot.KeyboardBuilder;
import io.vainslab.onemoresubscriber.bot.UserSession;
import io.vainslab.onemoresubscriber.bot.UserSession.UserState;
import io.vainslab.onemoresubscriber.entity.Subscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MakePaymentOperation implements ServiceOperation {

    @Override
    public String getCode() { return "pay"; }

    @Override
    public int getOrder() { return 3; }

    @Override
    public String getButtonLabel() {
        return "\uD83D\uDCB0 Внести платёж";
    }

    @Override
    public boolean isAvailable(Subscription subscription) {
        return subscription != null && subscription.getActive();
    }

    @Override
    public void execute(OperationContext ctx) {
        UserSession session = ctx.getSessionManager().getSession(ctx.getBotUser().getTelegramId());
        session.setState(UserState.AWAITING_PAYMENT_AMOUNT);
        session.setServiceId(ctx.getService().getId());
        session.setSubscriptionId(ctx.getSubscription().getId());

        ctx.reply("Введите сумму платежа (число):",
                KeyboardBuilder.backToServiceKeyboard(ctx.getService().getId()));
    }
}
