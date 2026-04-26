package io.vainslab.onemoresubscriber.operation;

import io.vainslab.onemoresubscriber.bot.KeyboardBuilder;
import io.vainslab.onemoresubscriber.entity.Subscription;
import io.vainslab.onemoresubscriber.handler.ServiceHandlerRegistry;
import io.vainslab.onemoresubscriber.handler.SubscriptionLifecycleHook;
import io.vainslab.onemoresubscriber.service.AuditService;
import io.vainslab.onemoresubscriber.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LeaveOperation implements ServiceOperation {

    private final SubscriptionService subscriptionService;
    private final AuditService auditService;
    private final ServiceHandlerRegistry handlerRegistry;

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
        SubscriptionLifecycleHook hook = handlerRegistry
                .getHandler(ctx.getService().getServiceType()).getLifecycleHook();
        if (hook != null) {
            try {
                hook.onLeave(ctx.getSubscription());
            } catch (Exception e) {
                log.error("Lifecycle hook onLeave failed for subscription={}", ctx.getSubscription().getId(), e);
            }
        }

        subscriptionService.leave(ctx.getSubscription().getId());
        auditService.logLeave(ctx.getBotUser(), ctx.getService().getName());
        ctx.reply("Вы покинули <b>" + ctx.getService().getName() + "</b>. До встречи!",
                KeyboardBuilder.backToListKeyboard());
    }
}
