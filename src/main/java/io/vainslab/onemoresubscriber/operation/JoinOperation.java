package io.vainslab.onemoresubscriber.operation;

import io.vainslab.onemoresubscriber.bot.KeyboardBuilder;
import io.vainslab.onemoresubscriber.bot.UserSession;
import io.vainslab.onemoresubscriber.bot.UserSession.UserState;
import io.vainslab.onemoresubscriber.entity.Subscription;
import io.vainslab.onemoresubscriber.service.AuditService;
import io.vainslab.onemoresubscriber.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JoinOperation implements ServiceOperation {

    private final SubscriptionService subscriptionService;
    private final AuditService auditService;

    @Override
    public String getCode() { return "join"; }

    @Override
    public int getOrder() { return 1; }

    @Override
    public String getButtonLabel() {
        return "\uD83D\uDE80 Присоединиться";
    }

    @Override
    public boolean isAvailable(Subscription subscription) {
        return subscription == null || !subscription.getActive();
    }

    @Override
    public void execute(OperationContext ctx) {
        int currentMembers = subscriptionService.countActiveMembers(ctx.getService().getId());
        if (currentMembers >= ctx.getService().getMaxMembers()) {
            ctx.reply("К сожалению, максимальное количество участников уже достигнуто.",
                    KeyboardBuilder.backToServiceKeyboard(ctx.getService().getId()));
            return;
        }

        String password = ctx.getService().getPassword();
        if (password != null && !password.isBlank()) {
            UserSession session = ctx.getSessionManager().getSession(ctx.getBotUser().getTelegramId());
            session.setState(UserState.AWAITING_SERVICE_PASSWORD);
            session.setServiceId(ctx.getService().getId());
            ctx.reply("\uD83D\uDD10 Для вступления введите пароль:",
                    KeyboardBuilder.backToServiceKeyboard(ctx.getService().getId()));
            return;
        }

        doJoin(ctx);
    }

    void doJoin(OperationContext ctx) {
        subscriptionService.join(ctx.getBotUser(), ctx.getService());
        auditService.logJoin(ctx.getBotUser(), ctx.getService().getName());
        ctx.reply("🎉 Добро пожаловать! Вы присоединились к <b>" + ctx.getService().getName() + "</b>.\nНажмите НАЗАД чтобы вернуться в меню.",
                KeyboardBuilder.backToServiceKeyboard(ctx.getService().getId()));
    }
}
