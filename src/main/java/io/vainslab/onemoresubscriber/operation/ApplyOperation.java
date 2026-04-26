package io.vainslab.onemoresubscriber.operation;

import io.vainslab.onemoresubscriber.bot.KeyboardBuilder;
import io.vainslab.onemoresubscriber.entity.BotUser;
import io.vainslab.onemoresubscriber.entity.Subscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApplyOperation implements ServiceOperation {

    @Override
    public String getCode() { return "apply"; }

    @Override
    public int getOrder() { return 0; }

    @Override
    public String getButtonLabel() {
        return "\uD83D\uDCE8 Отправить заявку";
    }

    @Override
    public boolean isAvailable(Subscription subscription) {
        return subscription == null || !subscription.getActive();
    }

    @Override
    public void execute(OperationContext ctx) {
        BotUser user = ctx.getBotUser();
        BotUser creator = ctx.getService().getCreatedBy();

        String userTag = user.getUsername() != null
                ? "@" + user.getUsername()
                : user.getFirstName();

        String text = "\uD83D\uDCE8 <b>Новая заявка</b> на вступление в <b>"
                + ctx.getService().getName() + "</b>\n\n"
                + "Пользователь: " + userTag + "\n"
                + "ID: <code>" + user.getTelegramId() + "</code>";

        ctx.getSender().send(creator.getTelegramId(), text);

        log.info("Application sent from user={} to creator={} for service={}",
                user.getTelegramId(), creator.getTelegramId(), ctx.getService().getName());

        ctx.reply("✅ Заявка отправлена создателю. Ожидайте ответа.",
                KeyboardBuilder.backToServiceKeyboard(ctx.getService().getId()));
    }
}
