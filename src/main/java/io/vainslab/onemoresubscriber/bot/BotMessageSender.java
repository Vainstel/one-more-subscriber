package io.vainslab.onemoresubscriber.bot;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
public class BotMessageSender {

    private final AbsSender sender;

    public BotMessageSender(AbsSender sender) {
        this.sender = sender;
    }

    public void send(Long chatId, String text) {
        send(chatId, text, null);
    }

    public void send(Long chatId, String text, InlineKeyboardMarkup keyboard) {
        try {
            SendMessage msg = SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(text)
                    .parseMode("HTML")
                    .replyMarkup(keyboard)
                    .build();
            sender.execute(msg);
        } catch (TelegramApiException e) {
            log.error("Failed to send message to chatId={}", chatId, e);
        }
    }

    public void editMessage(Long chatId, Integer messageId, String text, InlineKeyboardMarkup keyboard) {
        try {
            EditMessageText edit = EditMessageText.builder()
                    .chatId(chatId.toString())
                    .messageId(messageId)
                    .text(text)
                    .parseMode("HTML")
                    .replyMarkup(keyboard)
                    .build();
            sender.execute(edit);
        } catch (TelegramApiException e) {
            log.error("Failed to edit message chatId={}, messageId={}", chatId, messageId, e);
        }
    }
}
