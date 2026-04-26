package io.vainslab.onemoresubscriber.operation;

import io.vainslab.onemoresubscriber.bot.KeyboardBuilder;
import io.vainslab.onemoresubscriber.entity.Subscription;
import org.springframework.stereotype.Component;

@Component
public class SpeedTestOperation implements ServiceOperation {

    @Override
    public String getCode() { return "speedtest"; }

    @Override
    public int getOrder() { return 62; }

    @Override
    public String getButtonLabel() {
        return "\uD83D\uDCCA Тест скорости";
    }

    @Override
    public boolean isAvailable(Subscription subscription) {
        return subscription != null && subscription.getActive();
    }

    @Override
    public void execute(OperationContext ctx) {
        ctx.reply("📊 Функция в разработке. Обратитесь к администратору.",
                KeyboardBuilder.backToServiceKeyboard(ctx.getService().getId()));
    }
}
