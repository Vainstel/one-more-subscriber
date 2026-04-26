package io.vainslab.onemoresubscriber.operation;

import io.vainslab.onemoresubscriber.bot.KeyboardBuilder;
import io.vainslab.onemoresubscriber.entity.Subscription;
import org.springframework.stereotype.Component;

@Component
public class NewKeyOperation implements ServiceOperation {

    @Override
    public String getCode() { return "newkey"; }

    @Override
    public int getOrder() { return 61; }

    @Override
    public String getButtonLabel() {
        return "\uD83D\uDD11 Новый ключ";
    }

    @Override
    public boolean isAvailable(Subscription subscription) {
        return subscription != null && subscription.getActive();
    }

    @Override
    public void execute(OperationContext ctx) {
        ctx.reply("🔑 Функция в разработке. Обратитесь к администратору.",
                KeyboardBuilder.backToServiceKeyboard(ctx.getService().getId()));
    }
}
