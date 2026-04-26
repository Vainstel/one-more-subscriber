package io.vainslab.onemoresubscriber.operation;

import io.vainslab.onemoresubscriber.bot.KeyboardBuilder;
import io.vainslab.onemoresubscriber.bot.UserSession;
import io.vainslab.onemoresubscriber.bot.UserSession.UserState;
import io.vainslab.onemoresubscriber.entity.Subscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageCreatorOperation implements ServiceOperation {

    @Override
    public String getCode() { return "msg"; }

    @Override
    public int getOrder() { return 8; }

    @Override
    public String getButtonLabel() {
        return "✝️ Написать создателю";
    }

    @Override
    public boolean isAvailable(Subscription subscription) {
        return subscription != null && subscription.getActive();
    }

    @Override
    public void execute(OperationContext ctx) {
        UserSession session = ctx.getSessionManager().getSession(ctx.getBotUser().getTelegramId());
        session.setState(UserState.AWAITING_CREATOR_MESSAGE);
        session.setServiceId(ctx.getService().getId());
        session.setSubscriptionId(ctx.getSubscription().getId());

        ctx.reply("Введите сообщение для создателя сервиса:",
                KeyboardBuilder.backToServiceKeyboard(ctx.getService().getId()));
    }
}
