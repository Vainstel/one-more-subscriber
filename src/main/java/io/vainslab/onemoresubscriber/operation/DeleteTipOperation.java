package io.vainslab.onemoresubscriber.operation;

import io.vainslab.onemoresubscriber.bot.KeyboardBuilder;
import io.vainslab.onemoresubscriber.entity.Subscription;
import io.vainslab.onemoresubscriber.entity.TipPayment;
import io.vainslab.onemoresubscriber.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DeleteTipOperation implements ServiceOperation {

    private final PaymentService paymentService;

    @Override
    public String getCode() { return "deltip"; }

    @Override
    public int getOrder() { return 7; }

    @Override
    public String getButtonLabel() {
        return "\uD83D\uDDD1 Удалить чаевые";
    }

    @Override
    public boolean isAvailable(Subscription subscription) {
        return subscription != null && subscription.getActive();
    }

    @Override
    public void execute(OperationContext ctx) {
        List<TipPayment> deletable = paymentService.getDeletableTips(ctx.getSubscription().getId());

        if (deletable.isEmpty()) {
            ctx.reply("Нет чаевых, доступных для удаления.\n(Удалить можно только чаевые за последние 2 дня)",
                    KeyboardBuilder.backToServiceKeyboard(ctx.getService().getId()));
            return;
        }

        ctx.reply("Удалить можно только чаевые за последние 2 дня.\nВыберите для удаления:",
                KeyboardBuilder.deletableTipsKeyboard(ctx.getService().getId(), deletable));
    }
}
