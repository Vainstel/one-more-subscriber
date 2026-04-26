package io.vainslab.onemoresubscriber.operation;

import io.vainslab.onemoresubscriber.bot.KeyboardBuilder;
import io.vainslab.onemoresubscriber.entity.Payment;
import io.vainslab.onemoresubscriber.entity.Subscription;
import io.vainslab.onemoresubscriber.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DeletePaymentOperation implements ServiceOperation {

    private final PaymentService paymentService;

    @Override
    public String getCode() { return "delpay"; }

    @Override
    public int getOrder() { return 5; }

    @Override
    public String getButtonLabel() {
        return "\uD83D\uDDD1 Удалить платёж";
    }

    @Override
    public boolean isAvailable(Subscription subscription) {
        return subscription != null && subscription.getActive();
    }

    @Override
    public void execute(OperationContext ctx) {
        List<Payment> deletable = paymentService.getDeletablePayments(ctx.getSubscription().getId());

        if (deletable.isEmpty()) {
            ctx.reply("Нет платежей, доступных для удаления.\n(Удалить можно только платежи за последние 2 дня)",
                    KeyboardBuilder.backToServiceKeyboard(ctx.getService().getId()));
            return;
        }

        ctx.reply("Удалить можно только платежи за последние 2 дня.\nВыберите платёж для удаления:",
                KeyboardBuilder.deletablePaymentsKeyboard(ctx.getService().getId(), deletable));
    }
}
