package io.vainslab.onemoresubscriber.bot;

import io.vainslab.onemoresubscriber.entity.Payment;
import io.vainslab.onemoresubscriber.entity.Service;
import io.vainslab.onemoresubscriber.entity.TipPayment;
import io.vainslab.onemoresubscriber.operation.ServiceOperation;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class KeyboardBuilder {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private KeyboardBuilder() {
    }

    public static InlineKeyboardMarkup serviceListKeyboard(List<Service> services) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Service s : services) {
            rows.add(List.of(InlineKeyboardButton.builder()
                    .text(s.getName())
                    .callbackData(CallbackPrefix.SERVICE + s.getId())
                    .build()));
        }
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public static InlineKeyboardMarkup operationsKeyboard(Long serviceId,
                                                           List<ServiceOperation> operations) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (ServiceOperation op : operations) {
            rows.add(List.of(InlineKeyboardButton.builder()
                    .text(op.getButtonLabel())
                    .callbackData(CallbackPrefix.OPERATION + serviceId + ":" + op.getCode())
                    .build()));
        }
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public static InlineKeyboardMarkup deletablePaymentsKeyboard(Long serviceId,
                                                                   List<Payment> payments) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Payment p : payments) {
            String label = p.getAmount() + " " + p.getCurrency() + " — " + p.getCreatedAt().format(FMT);
            rows.add(List.of(InlineKeyboardButton.builder()
                    .text(label)
                    .callbackData(CallbackPrefix.DELETE_PAYMENT + p.getId())
                    .build()));
        }
        rows.add(backToServiceRow(serviceId));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public static InlineKeyboardMarkup deletableTipsKeyboard(Long serviceId,
                                                              List<TipPayment> tips) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (TipPayment t : tips) {
            String label = t.getAmount() + " — " + t.getCreatedAt().format(FMT);
            rows.add(List.of(InlineKeyboardButton.builder()
                    .text(label)
                    .callbackData(CallbackPrefix.DELETE_TIP + t.getId())
                    .build()));
        }
        rows.add(backToServiceRow(serviceId));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public static InlineKeyboardMarkup backToServiceKeyboard(Long serviceId) {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(backToServiceRow(serviceId)))
                .build();
    }

    public static InlineKeyboardMarkup backToListKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(List.of(InlineKeyboardButton.builder()
                        .text("◀\uFE0F Назад")
                        .callbackData(CallbackPrefix.BACK)
                        .build())))
                .build();
    }

    private static List<InlineKeyboardButton> backToServiceRow(Long serviceId) {
        return List.of(InlineKeyboardButton.builder()
                .text("◀\uFE0F Назад")
                .callbackData(CallbackPrefix.BACK_TO_SERVICE + serviceId)
                .build());
    }
}
