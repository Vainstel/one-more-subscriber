package io.vainslab.onemoresubscriber.operation;

import io.vainslab.onemoresubscriber.bot.KeyboardBuilder;
import io.vainslab.onemoresubscriber.entity.Subscription;
import io.vainslab.onemoresubscriber.service.AuditService;
import io.vainslab.onemoresubscriber.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LeaveOperation implements ServiceOperation {

    private final SubscriptionService subscriptionService;
    private final AuditService auditService;

    @Override
    public String getCode() { return "leave"; }

    @Override
    public int getOrder() { return 2; }

    @Override
    public String getButtonLabel() {
        return "\uD83D\uDEAA Покинуть подписку";
    }

    @Override
    public boolean isAvailable(Subscription subscription) {
        if (subscription == null || !subscription.getActive()) return false;
        if (subscription.isInGracePeriod()) return true;
        return !subscription.isInDebt();
    }

    @Override
    public void execute(OperationContext ctx) {
        subscriptionService.leave(ctx.getSubscription().getId());
        auditService.logLeave(ctx.getBotUser(), ctx.getService().getName());
        ctx.reply("Вы покинули <b>" + ctx.getService().getName() + "</b>. До встречи!",
                KeyboardBuilder.backToListKeyboard());
    }
}
