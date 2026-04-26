package io.vainslab.onemoresubscriber.operation;

import io.vainslab.onemoresubscriber.bot.BotMessageSender;
import io.vainslab.onemoresubscriber.bot.UserSessionManager;
import io.vainslab.onemoresubscriber.entity.BotUser;
import io.vainslab.onemoresubscriber.entity.Subscription;
import lombok.Builder;
import lombok.Getter;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Getter
@Builder
public class OperationContext {

    private final Long chatId;
    private final Integer messageId;
    private final BotUser botUser;
    private final io.vainslab.onemoresubscriber.entity.Service service;
    private final Subscription subscription;
    private final BotMessageSender sender;
    private final UserSessionManager sessionManager;

    public void reply(String text, InlineKeyboardMarkup keyboard) {
        if (messageId != null) {
            sender.editMessage(chatId, messageId, text, keyboard);
        } else {
            sender.send(chatId, text, keyboard);
        }
    }

    public void reply(String text) {
        reply(text, null);
    }
}
