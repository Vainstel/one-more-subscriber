package io.vainslab.onemoresubscriber.bot;

import io.vainslab.onemoresubscriber.entity.AuditLog;
import io.vainslab.onemoresubscriber.entity.BalanceDeduction;
import io.vainslab.onemoresubscriber.entity.BotUser;
import io.vainslab.onemoresubscriber.entity.Payment;
import io.vainslab.onemoresubscriber.entity.Service;
import io.vainslab.onemoresubscriber.entity.Subscription;
import io.vainslab.onemoresubscriber.handler.ServiceHandlerRegistry;
import io.vainslab.onemoresubscriber.handler.SubscriptionLifecycleHook;
import io.vainslab.onemoresubscriber.repository.ServiceRepository;
import io.vainslab.onemoresubscriber.service.AuditService;
import io.vainslab.onemoresubscriber.service.PaymentService;
import io.vainslab.onemoresubscriber.service.ServiceReportBuilder;
import io.vainslab.onemoresubscriber.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminHandler {

    private static final int USERS_PER_PAGE = 5;
    private static final int PAYMENTS_SHOWN = 10;
    private static final int AUDIT_SHOWN = 15;
    private static final int AUDIT_ACTION_MAX_LEN = 120;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd.MM HH:mm");

    private final ServiceRepository serviceRepository;
    private final SubscriptionService subscriptionService;
    private final PaymentService paymentService;
    private final ServiceReportBuilder reportBuilder;
    private final AuditService auditService;
    private final ServiceHandlerRegistry handlerRegistry;

    public boolean handleCallback(String data, Long chatId, Integer messageId, BotMessageSender sender, BotUser adminUser) {
        if (data.startsWith(CallbackPrefix.ADMIN_SERVICE)) {
            Long serviceId = Long.parseLong(data.substring(CallbackPrefix.ADMIN_SERVICE.length()));
            showAdminServiceMenu(chatId, messageId, serviceId, sender);
            return true;
        }
        if (data.startsWith(CallbackPrefix.ADMIN_REPORT_WEEK)) {
            Long serviceId = Long.parseLong(data.substring(CallbackPrefix.ADMIN_REPORT_WEEK.length()));
            showReport(chatId, messageId, serviceId, 7, sender);
            return true;
        }
        if (data.startsWith(CallbackPrefix.ADMIN_REPORT_MONTH)) {
            Long serviceId = Long.parseLong(data.substring(CallbackPrefix.ADMIN_REPORT_MONTH.length()));
            showReport(chatId, messageId, serviceId, 30, sender);
            return true;
        }
        if (data.startsWith(CallbackPrefix.ADMIN_USERS)) {
            String[] parts = data.substring(CallbackPrefix.ADMIN_USERS.length()).split(":");
            Long serviceId = Long.parseLong(parts[0]);
            int page = Integer.parseInt(parts[1]);
            showUsersPage(chatId, messageId, serviceId, page, sender);
            return true;
        }
        if (data.startsWith(CallbackPrefix.ADMIN_USER_DETAIL)) {
            Long subId = Long.parseLong(data.substring(CallbackPrefix.ADMIN_USER_DETAIL.length()));
            showUserDetail(chatId, messageId, subId, sender);
            return true;
        }
        if (data.startsWith(CallbackPrefix.ADMIN_KICK)) {
            Long subId = Long.parseLong(data.substring(CallbackPrefix.ADMIN_KICK.length()));
            showKickConfirm(chatId, messageId, subId, sender);
            return true;
        }
        if (data.startsWith(CallbackPrefix.ADMIN_KICK_CONFIRM)) {
            Long subId = Long.parseLong(data.substring(CallbackPrefix.ADMIN_KICK_CONFIRM.length()));
            executeKick(chatId, messageId, subId, sender, adminUser);
            return true;
        }
        if (data.startsWith(CallbackPrefix.ADMIN_BILLING_PAUSE)) {
            Long serviceId = Long.parseLong(data.substring(CallbackPrefix.ADMIN_BILLING_PAUSE.length()));
            pauseBilling(chatId, messageId, serviceId, sender, adminUser);
            return true;
        }
        if (data.startsWith(CallbackPrefix.ADMIN_BILLING_RESUME)) {
            Long serviceId = Long.parseLong(data.substring(CallbackPrefix.ADMIN_BILLING_RESUME.length()));
            showResumeConfirm(chatId, messageId, serviceId, sender);
            return true;
        }
        if (data.startsWith(CallbackPrefix.ADMIN_BILLING_RESUME_CONFIRM)) {
            Long serviceId = Long.parseLong(data.substring(CallbackPrefix.ADMIN_BILLING_RESUME_CONFIRM.length()));
            resumeBilling(chatId, messageId, serviceId, sender, adminUser);
            return true;
        }
        return false;
    }

    public void showAdminMenu(Long chatId, Integer messageId, BotMessageSender sender) {
        List<Service> services = serviceRepository.findAllByActiveTrue();
        if (services.isEmpty()) {
            sendOrEdit(chatId, messageId, "Нет активных сервисов.", null, sender);
            return;
        }

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Service s : services) {
            rows.add(List.of(btn("\uD83D\uDD27 " + s.getName(), CallbackPrefix.ADMIN_SERVICE + s.getId())));
        }

        sendOrEdit(chatId, messageId, "<b>Админ-панель</b>\nВыберите сервис:",
                InlineKeyboardMarkup.builder().keyboard(rows).build(), sender);
    }

    private void showAdminServiceMenu(Long chatId, Integer messageId, Long serviceId, BotMessageSender sender) {
        Service service = serviceRepository.findById(serviceId).orElse(null);
        if (service == null) return;

        int memberCount = subscriptionService.countActiveMembers(serviceId);
        String billingStatus = service.getBillingActive()
                ? "✅ Списание активно"
                : "⏸ Списание приостановлено";

        String text = "<b>\uD83D\uDD27 " + service.getName() + "</b>\n\n"
                + "\uD83D\uDC65 Участников: " + memberCount + "/" + service.getMaxMembers() + "\n"
                + billingStatus;

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(List.of(btn("\uD83D\uDCCA Отчёт за неделю", CallbackPrefix.ADMIN_REPORT_WEEK + serviceId)));
        rows.add(List.of(btn("\uD83D\uDCCA Отчёт за месяц", CallbackPrefix.ADMIN_REPORT_MONTH + serviceId)));
        rows.add(List.of(btn("\uD83D\uDC65 Участники", CallbackPrefix.ADMIN_USERS + serviceId + ":0")));

        if (service.getBillingActive()) {
            rows.add(List.of(btn("⏸ Приостановить списание", CallbackPrefix.ADMIN_BILLING_PAUSE + serviceId)));
        } else {
            rows.add(List.of(btn("▶\uFE0F Возобновить списание", CallbackPrefix.ADMIN_BILLING_RESUME + serviceId)));
        }

        rows.add(List.of(btn("◀\uFE0F Назад", CallbackPrefix.ADMIN_BACK)));

        sendOrEdit(chatId, messageId, text,
                InlineKeyboardMarkup.builder().keyboard(rows).build(), sender);
    }

    private void showReport(Long chatId, Integer messageId, Long serviceId, int days, BotMessageSender sender) {
        Service service = serviceRepository.findById(serviceId).orElse(null);
        if (service == null) return;

        LocalDateTime since = LocalDateTime.now().minusDays(days);
        String periodLabel = days == 7
                ? "Неделя: " + since.format(FMT) + " — " + LocalDateTime.now().format(FMT)
                : "Месяц: " + since.format(FMT) + " — " + LocalDateTime.now().format(FMT);

        String report = reportBuilder.buildReport(service, since, periodLabel);

        List<List<InlineKeyboardButton>> rows = List.of(
                List.of(btn("◀\uFE0F Назад", "adm:" + serviceId))
        );

        sendOrEdit(chatId, messageId, report,
                InlineKeyboardMarkup.builder().keyboard(rows).build(), sender);
    }

    private void showUsersPage(Long chatId, Integer messageId, Long serviceId, int page, BotMessageSender sender) {
        List<Subscription> allSubs = subscriptionService.findAllByService(serviceId);
        if (allSubs.isEmpty()) {
            sendOrEdit(chatId, messageId, "Нет участников.",
                    InlineKeyboardMarkup.builder().keyboard(List.of(
                            List.of(btn("◀\uFE0F Назад", "adm:" + serviceId))
                    )).build(), sender);
            return;
        }

        int totalPages = (allSubs.size() + USERS_PER_PAGE - 1) / USERS_PER_PAGE;
        page = Math.max(0, Math.min(page, totalPages - 1));
        int from = page * USERS_PER_PAGE;
        int to = Math.min(from + USERS_PER_PAGE, allSubs.size());
        List<Subscription> pageSubs = allSubs.subList(from, to);

        StringBuilder sb = new StringBuilder();
        sb.append("<b>\uD83D\uDC65 Участники</b> (").append(page + 1).append("/").append(totalPages).append(")\n\n");

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Subscription sub : pageSubs) {
            String status = sub.getActive() ? "✅" : "❌";
            String label = status + " " + sub.getUser().getDisplayName();
            if (sub.getPaidUntil() != null && sub.getActive()) {
                if (sub.isInDebt()) {
                    long days = ChronoUnit.DAYS.between(sub.getPaidUntil().toLocalDate(), LocalDate.now());
                    label += " ⚠️-" + days + "дн";
                } else {
                    long days = ChronoUnit.DAYS.between(LocalDate.now(), sub.getPaidUntil().toLocalDate());
                    label += " +" + days + "дн";
                }
            }
            rows.add(List.of(btn(label, CallbackPrefix.ADMIN_USER_DETAIL + sub.getId())));
        }

        // Pagination
        List<InlineKeyboardButton> navRow = new ArrayList<>();
        if (page > 0) {
            navRow.add(btn("◀ Пред", CallbackPrefix.ADMIN_USERS + serviceId + ":" + (page - 1)));
        }
        if (page < totalPages - 1) {
            navRow.add(btn("След ▶", CallbackPrefix.ADMIN_USERS + serviceId + ":" + (page + 1)));
        }
        if (!navRow.isEmpty()) {
            rows.add(navRow);
        }

        rows.add(List.of(btn("◀\uFE0F Назад", "adm:" + serviceId)));

        sendOrEdit(chatId, messageId, sb.toString(),
                InlineKeyboardMarkup.builder().keyboard(rows).build(), sender);
    }

    private void showUserDetail(Long chatId, Integer messageId, Long subId, BotMessageSender sender) {
        Subscription sub = subscriptionService.findById(subId).orElse(null);
        if (sub == null) return;

        StringBuilder sb = new StringBuilder();
        sb.append("<b>").append(sub.getUser().getDisplayName()).append("</b>\n\n");
        sb.append("Статус: ").append(sub.getActive() ? "активен ✅" : "неактивен ❌").append("\n");
        sb.append("Вступил: ").append(sub.getJoinedAt().format(FMT)).append("\n");

        if (sub.getPaidUntil() != null) {
            sb.append("Оплачено до: ").append(sub.getPaidUntil().format(FMT)).append("\n");
            if (sub.getActive()) {
                if (sub.isInDebt()) {
                    long days = ChronoUnit.DAYS.between(sub.getPaidUntil().toLocalDate(), LocalDate.now());
                    sb.append("⚠️ Просрочка: ").append(days).append(" дн.\n");
                } else {
                    long days = ChronoUnit.DAYS.between(LocalDate.now(), sub.getPaidUntil().toLocalDate());
                    sb.append("✅ Осталось: ").append(days).append(" дн.\n");
                }
            }
        }

        if (sub.getActive()) {
            sb.append("💰 Баланс: <b>").append(sub.getBalance()).append(" ").append(sub.getService().getCurrency()).append("</b>\n");
        }

        if (sub.getDeactivatedAt() != null) {
            sb.append("Деактивирован: ").append(sub.getDeactivatedAt().format(FMT)).append("\n");
        }

        // Пополнения
        List<Payment> payments = paymentService.getPayments(sub.getId());
        sb.append("\n\uD83D\uDCB0 <b>Пополнения:</b>\n");
        if (payments.isEmpty()) {
            sb.append("  пока нет\n");
        } else {
            int shown = Math.min(payments.size(), PAYMENTS_SHOWN);
            for (int i = 0; i < shown; i++) {
                Payment p = payments.get(i);
                sb.append("  ").append(p.getAmount()).append(" ").append(p.getCurrency())
                        .append(" — ").append(p.getCreatedAt().format(DATETIME_FMT)).append("\n");
            }
            if (payments.size() > PAYMENTS_SHOWN) {
                sb.append("  …и ещё ").append(payments.size() - PAYMENTS_SHOWN).append("\n");
            }
        }

        // Списания
        int maxMembers = sub.getService().getMaxMembers();
        var monthlyCost = sub.getService().getMonthlyCost();
        List<BalanceDeduction> deductions = paymentService.getDeductions(sub.getId());
        sb.append("\n\uD83D\uDCC9 <b>Списания:</b>\n");
        sb.append("<i>сумма — дата участники/макс/мес</i>\n");
        if (deductions.isEmpty()) {
            sb.append("  пока нет\n");
        } else {
            int shown = Math.min(deductions.size(), PAYMENTS_SHOWN);
            for (int i = 0; i < shown; i++) {
                BalanceDeduction d = deductions.get(i);
                sb.append("  ").append(d.getAmount()).append(" — ")
                        .append(d.getCreatedAt().format(DATETIME_FMT)).append(" ")
                        .append(d.getMemberCount()).append("/").append(maxMembers)
                        .append("/").append(monthlyCost)
                        .append("\n");
            }
            if (deductions.size() > PAYMENTS_SHOWN) {
                sb.append("  …и ещё ").append(deductions.size() - PAYMENTS_SHOWN).append("\n");
            }
        }

        // Audit
        List<AuditLog> auditLogs = auditService.getRecentByUser(sub.getUser().getId(), AUDIT_SHOWN);
        sb.append("\n\uD83D\uDCDD <b>Последние события:</b>\n");
        if (auditLogs.isEmpty()) {
            sb.append("  пока нет\n");
        } else {
            for (AuditLog log : auditLogs) {
                String action = log.getAction();
                if (action.length() > AUDIT_ACTION_MAX_LEN) {
                    action = action.substring(0, AUDIT_ACTION_MAX_LEN) + "…";
                }
                sb.append("  ").append(log.getCreatedAt().format(DATETIME_FMT))
                        .append(" — ").append(action).append("\n");
            }
        }

        Long serviceId = sub.getService().getId();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        if (sub.getActive()) {
            rows.add(List.of(btn("🚫 Исключить из подписки", CallbackPrefix.ADMIN_KICK + subId)));
        }
        rows.add(List.of(btn("◀\uFE0F Назад", CallbackPrefix.ADMIN_USERS + serviceId + ":0")));

        sendOrEdit(chatId, messageId, sb.toString(),
                InlineKeyboardMarkup.builder().keyboard(rows).build(), sender);
    }

    private void showKickConfirm(Long chatId, Integer messageId, Long subId, BotMessageSender sender) {
        Subscription sub = subscriptionService.findById(subId).orElse(null);
        if (sub == null) return;

        String warning = "Вы уверены, что хотите исключить <b>" + sub.getUser().getDisplayName() + "</b>?";
        if (sub.isInDebt()) {
            warning += "\n\n⚠️ У пользователя отрицательный баланс — он будет обнулён.";
        }

        Long serviceId = sub.getService().getId();
        List<List<InlineKeyboardButton>> rows = List.of(
                List.of(btn("✅ Да, исключить", CallbackPrefix.ADMIN_KICK_CONFIRM + subId)),
                List.of(btn("❌ Отмена", "a:ud:" + subId))
        );

        sendOrEdit(chatId, messageId, warning,
                InlineKeyboardMarkup.builder().keyboard(rows).build(), sender);
    }

    private void executeKick(Long chatId, Integer messageId, Long subId, BotMessageSender sender, BotUser adminUser) {
        Subscription sub = subscriptionService.findById(subId).orElse(null);
        if (sub == null) return;

        Long serviceId = sub.getService().getId();
        String userName = sub.getUser().getDisplayName();

        SubscriptionLifecycleHook hook = handlerRegistry
                .getHandler(sub.getService().getServiceType()).getLifecycleHook();
        if (hook != null) {
            try {
                hook.onLeave(sub);
            } catch (Exception e) {
                log.error("Lifecycle hook onLeave failed on kick for subscription={}", subId, e);
            }
        }

        subscriptionService.kick(subId);
        auditService.logKick(adminUser, sub.getUser(), sub.getService().getName());

        sendOrEdit(chatId, messageId,
                "🚫 <b>" + userName + "</b> исключён из подписки.",
                InlineKeyboardMarkup.builder().keyboard(List.of(
                        List.of(btn("◀\uFE0F К участникам", CallbackPrefix.ADMIN_USERS + serviceId + ":0"))
                )).build(), sender);
    }

    private void pauseBilling(Long chatId, Integer messageId, Long serviceId,
                              BotMessageSender sender, BotUser adminUser) {
        Service service = serviceRepository.findById(serviceId).orElse(null);
        if (service == null) return;

        subscriptionService.setBillingActive(serviceId, false);
        auditService.logBillingToggle(adminUser, service.getName(), false);
        showAdminServiceMenu(chatId, messageId, serviceId, sender);
    }

    private void showResumeConfirm(Long chatId, Integer messageId, Long serviceId, BotMessageSender sender) {
        Service service = serviceRepository.findById(serviceId).orElse(null);
        if (service == null) return;

        int memberCount = subscriptionService.countActiveMembers(serviceId);
        String text = "⚠\uFE0F Возобновить списание для <b>" + service.getName() + "</b>?\n\n"
                + "Дата последнего списания будет сброшена для всех " + memberCount
                + " активных участников. Списание начнётся со следующего дня.";

        List<List<InlineKeyboardButton>> rows = List.of(
                List.of(btn("✅ Да, возобновить", CallbackPrefix.ADMIN_BILLING_RESUME_CONFIRM + serviceId)),
                List.of(btn("❌ Отмена", "adm:" + serviceId))
        );

        sendOrEdit(chatId, messageId, text,
                InlineKeyboardMarkup.builder().keyboard(rows).build(), sender);
    }

    private void resumeBilling(Long chatId, Integer messageId, Long serviceId,
                               BotMessageSender sender, BotUser adminUser) {
        Service service = serviceRepository.findById(serviceId).orElse(null);
        if (service == null) return;

        subscriptionService.setBillingActive(serviceId, true);
        auditService.logBillingToggle(adminUser, service.getName(), true);
        showAdminServiceMenu(chatId, messageId, serviceId, sender);
    }

    private void sendOrEdit(Long chatId, Integer messageId, String text,
                            InlineKeyboardMarkup keyboard, BotMessageSender sender) {
        if (messageId != null) {
            sender.editMessage(chatId, messageId, text, keyboard);
        } else {
            sender.send(chatId, text, keyboard);
        }
    }

    private InlineKeyboardButton btn(String text, String callbackData) {
        return InlineKeyboardButton.builder().text(text).callbackData(callbackData).build();
    }
}
