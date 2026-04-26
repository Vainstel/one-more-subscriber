package io.vainslab.onemoresubscriber.bot;

import io.vainslab.onemoresubscriber.bot.UserSession.UserState;
import io.vainslab.onemoresubscriber.config.BotProperties;
import io.vainslab.onemoresubscriber.entity.*;
import io.vainslab.onemoresubscriber.handler.ServiceHandler;
import io.vainslab.onemoresubscriber.handler.ServiceHandlerRegistry;
import io.vainslab.onemoresubscriber.operation.OperationContext;
import io.vainslab.onemoresubscriber.operation.ServiceOperation;
import io.vainslab.onemoresubscriber.repository.CreatorMessageRepository;
import io.vainslab.onemoresubscriber.repository.PaymentRepository;
import io.vainslab.onemoresubscriber.repository.ServiceRepository;
import io.vainslab.onemoresubscriber.repository.TipPaymentRepository;
import io.vainslab.onemoresubscriber.service.*;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Slf4j
public class SubscriptionBot extends TelegramLongPollingBot {

    private final BotProperties botProperties;
    private final BotUserService botUserService;
    private final SubscriptionService subscriptionService;
    private final PaymentService paymentService;
    private final ErrorLoggingService errorLoggingService;
    private final ServiceRepository serviceRepository;
    private final PaymentRepository paymentRepository;
    private final TipPaymentRepository tipPaymentRepository;
    private final CreatorMessageRepository creatorMessageRepository;
    private final ServiceHandlerRegistry handlerRegistry;
    private final UserSessionManager sessionManager;
    private final AdminHandler adminHandler;
    private final AuditService auditService;
    private final BotMessageSender messageSender;

    public SubscriptionBot(BotProperties botProperties,
                           BotUserService botUserService,
                           SubscriptionService subscriptionService,
                           PaymentService paymentService,
                           ErrorLoggingService errorLoggingService,
                           ServiceRepository serviceRepository,
                           PaymentRepository paymentRepository,
                           TipPaymentRepository tipPaymentRepository,
                           CreatorMessageRepository creatorMessageRepository,
                           ServiceHandlerRegistry handlerRegistry,
                           UserSessionManager sessionManager,
                           AdminHandler adminHandler,
                           AuditService auditService) {
        super(botProperties.getToken());
        this.botProperties = botProperties;
        this.botUserService = botUserService;
        this.subscriptionService = subscriptionService;
        this.paymentService = paymentService;
        this.errorLoggingService = errorLoggingService;
        this.serviceRepository = serviceRepository;
        this.paymentRepository = paymentRepository;
        this.tipPaymentRepository = tipPaymentRepository;
        this.creatorMessageRepository = creatorMessageRepository;
        this.handlerRegistry = handlerRegistry;
        this.sessionManager = sessionManager;
        this.adminHandler = adminHandler;
        this.auditService = auditService;
        this.messageSender = new BotMessageSender(this);
        registerCommands();
    }

    @Override
    public String getBotUsername() {
        return "one_more_subscriber_bot";
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasCallbackQuery()) {
                handleCallback(update.getCallbackQuery());
            } else if (update.hasMessage() && update.getMessage().hasText()) {
                handleMessage(update.getMessage());
            }
        } catch (Exception e) {
            log.error("Error processing update", e);
            BotUser user = null;
            Long chatId = null;
            try {
                if (update.hasMessage()) {
                    user = botUserService.getOrCreate(update.getMessage().getFrom());
                    chatId = update.getMessage().getChatId();
                } else if (update.hasCallbackQuery()) {
                    user = botUserService.getOrCreate(update.getCallbackQuery().getFrom());
                    chatId = update.getCallbackQuery().getMessage().getChatId();
                }
                String action = update.hasCallbackQuery()
                        ? "callback:" + update.getCallbackQuery().getData()
                        : "message";
                errorLoggingService.logError(user, action, e);
                auditService.logError(user, action, e);
            } catch (Exception inner) {
                log.error("Failed to log error", inner);
            }
            if (chatId != null) {
                messageSender.send(chatId, "Что-то пошло не так, попробуйте позже \uD83D\uDE14");
            }
        }
    }

    private void handleMessage(Message message) {
        BotUser user = botUserService.getOrCreate(message.getFrom());
        Long chatId = message.getChatId();
        String text = message.getText().trim();

        if (text.equals("/start") || text.equals("/menu")) {
            sessionManager.resetSession(user.getTelegramId());
            showServiceList(chatId, null);
            return;
        }

        if (text.equals("/admin") && botProperties.isAdmin(user.getTelegramId())) {
            adminHandler.showAdminMenu(chatId, null, messageSender);
            return;
        }

        UserSession session = sessionManager.getSession(user.getTelegramId());

        switch (session.getState()) {
            case AWAITING_SERVICE_PASSWORD -> handleServicePasswordInput(chatId, user, session, text);
            case AWAITING_PAYMENT_AMOUNT -> handlePaymentInput(chatId, user, session, text);
            case AWAITING_TIP_AMOUNT -> handleTipInput(chatId, user, session, text);
            case AWAITING_CREATOR_MESSAGE -> handleCreatorMessageInput(chatId, user, session, text);
            default -> messageSender.send(chatId, "Используйте /start для начала работы.");
        }
    }

    private void handleCallback(CallbackQuery callback) {
        answerCallback(callback.getId());
        BotUser user = botUserService.getOrCreate(callback.getFrom());
        Long chatId = callback.getMessage().getChatId();
        Integer messageId = callback.getMessage().getMessageId();
        String data = callback.getData();
        sessionManager.resetSession(user.getTelegramId());

        if (data.equals(CallbackPrefix.BACK)) {
            showServiceList(chatId, messageId);
            return;
        }

        if (data.startsWith(CallbackPrefix.BACK_TO_SERVICE)) {
            Long serviceId = Long.parseLong(data.substring(CallbackPrefix.BACK_TO_SERVICE.length()));
            showServiceMenu(chatId, messageId, user, serviceId);
            return;
        }

        if (data.startsWith(CallbackPrefix.SERVICE)) {
            Long serviceId = Long.parseLong(data.substring(CallbackPrefix.SERVICE.length()));
            showServiceMenu(chatId, messageId, user, serviceId);
            return;
        }

        if (data.startsWith(CallbackPrefix.OPERATION)) {
            String[] parts = data.split(":");
            Long serviceId = Long.parseLong(parts[1]);
            String opCode = parts[2];
            executeOperation(chatId, messageId, user, serviceId, opCode);
            return;
        }

        if (data.startsWith(CallbackPrefix.DELETE_PAYMENT)) {
            Long paymentId = Long.parseLong(data.substring(CallbackPrefix.DELETE_PAYMENT.length()));
            handleDeletePayment(chatId, messageId, user, paymentId);
            return;
        }

        if (data.startsWith(CallbackPrefix.DELETE_TIP)) {
            Long tipId = Long.parseLong(data.substring(CallbackPrefix.DELETE_TIP.length()));
            handleDeleteTip(chatId, messageId, user, tipId);
            return;
        }

        // Admin callbacks
        if (data.startsWith(CallbackPrefix.ADMIN_SERVICE) || data.startsWith("a:")) {
            if (data.equals(CallbackPrefix.ADMIN_BACK)) {
                adminHandler.showAdminMenu(chatId, messageId, messageSender);
            } else {
                adminHandler.handleCallback(data, chatId, messageId, messageSender, user);
            }
        }
    }

    private void showServiceList(Long chatId, Integer messageId) {
        List<Service> services = serviceRepository.findAllByActiveTrue();
        if (services.isEmpty()) {
            sendOrEdit(chatId, messageId, "Пока ни одного сервиса не добавлено. Загляните позже!", null);
            return;
        }
        sendOrEdit(chatId, messageId,
                "Выберите сервис для управления подпиской:",
                KeyboardBuilder.serviceListKeyboard(services));
    }

    private void showServiceMenu(Long chatId, Integer messageId, BotUser user, Long serviceId) {
        Service service = serviceRepository.findById(serviceId).orElse(null);
        if (service == null) {
            sendOrEdit(chatId, messageId, "Сервис не найден.", null);
            return;
        }

        Subscription subscription = subscriptionService
                .findByUserAndService(user.getId(), serviceId).orElse(null);
        int memberCount = subscriptionService.countActiveMembers(serviceId);

        BigDecimal userBalance = (subscription != null && subscription.getActive())
                ? subscription.getBalance()
                : null;

        ServiceHandler handler = handlerRegistry.getHandler(service.getServiceType());
        String info = handler.buildServiceInfoMessage(service, subscription, memberCount, userBalance);
        List<ServiceOperation> available = handler.getAvailableOperations(subscription);

        sendOrEdit(chatId, messageId, info,
                KeyboardBuilder.operationsKeyboard(serviceId, available));
    }

    private void executeOperation(Long chatId, Integer messageId, BotUser user,
                                   Long serviceId, String opCode) {
        Service service = serviceRepository.findById(serviceId).orElse(null);
        if (service == null) return;

        Subscription subscription = subscriptionService
                .findByUserAndService(user.getId(), serviceId).orElse(null);

        if (opCode.equals("back")) {
            showServiceList(chatId, messageId);
            return;
        }

        ServiceHandler handler = handlerRegistry.getHandler(service.getServiceType());
        Optional<ServiceOperation> operation = handler.getAllOperations().stream()
                .filter(op -> op.getCode().equals(opCode))
                .findFirst();

        if (operation.isEmpty()) return;

        OperationContext ctx = OperationContext.builder()
                .chatId(chatId)
                .messageId(messageId)
                .botUser(user)
                .service(service)
                .subscription(subscription)
                .sender(messageSender)
                .sessionManager(sessionManager)
                .build();

        operation.get().execute(ctx);
    }

    private void handleServicePasswordInput(Long chatId, BotUser user, UserSession session, String text) {
        Service service = serviceRepository.findById(session.getServiceId()).orElse(null);
        if (service == null) {
            sessionManager.resetSession(user.getTelegramId());
            messageSender.send(chatId, "Сервис не найден.");
            return;
        }

        if (!service.getPassword().equals(text.trim())) {
            messageSender.send(chatId, "\u274C Неверный пароль. Попробуйте ещё раз:",
                    KeyboardBuilder.backToServiceKeyboard(service.getId()));
            return;
        }

        int currentMembers = subscriptionService.countActiveMembers(service.getId());
        if (currentMembers >= service.getMaxMembers()) {
            sessionManager.resetSession(user.getTelegramId());
            messageSender.send(chatId, "К сожалению, максимальное количество участников уже достигнуто.",
                    KeyboardBuilder.backToServiceKeyboard(service.getId()));
            return;
        }

        subscriptionService.join(user, service);
        auditService.logJoin(user, service.getName());
        sessionManager.resetSession(user.getTelegramId());
        messageSender.send(chatId,
                "🎉 Добро пожаловать! Вы присоединились к <b>" + service.getName() + "</b>.\nНажмите НАЗАД чтобы вернуться в меню.",
                KeyboardBuilder.backToServiceKeyboard(service.getId()));
    }

    private void handlePaymentInput(Long chatId, BotUser user, UserSession session, String text) {
        BigDecimal amount;
        try {
            amount = new BigDecimal(text.replace(",", "."));
        } catch (NumberFormatException e) {
            messageSender.send(chatId, "Некорректная сумма. Введите число:");
            return;
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            messageSender.send(chatId, "Сумма должна быть положительной. Попробуйте снова:");
            return;
        }

        Subscription sub = subscriptionService
                .findByUserAndService(user.getId(), session.getServiceId())
                .orElse(null);
        if (sub == null) {
            messageSender.send(chatId, "Подписка не найдена.");
            sessionManager.resetSession(user.getTelegramId());
            return;
        }

        paymentService.addPayment(sub.getId(), amount);
        auditService.logPayment(user, sub.getService().getName(), amount, sub.getService().getCurrency());
        sessionManager.resetSession(user.getTelegramId());
        messageSender.send(chatId,
                "\u2705 Платёж на сумму <b>" + amount + " " + sub.getService().getCurrency() + "</b> записан!",
                KeyboardBuilder.backToServiceKeyboard(sub.getService().getId()));
    }

    private void handleTipInput(Long chatId, BotUser user, UserSession session, String text) {
        BigDecimal amount;
        try {
            amount = new BigDecimal(text.replace(",", "."));
        } catch (NumberFormatException e) {
            messageSender.send(chatId, "Некорректная сумма. Введите число:");
            return;
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            messageSender.send(chatId, "Сумма должна быть положительной.");
            return;
        }

        Subscription sub = subscriptionService
                .findByUserAndService(user.getId(), session.getServiceId())
                .orElse(null);
        if (sub == null) {
            sessionManager.resetSession(user.getTelegramId());
            return;
        }

        if (!paymentService.canTip(sub.getId(), amount)) {
            messageSender.send(chatId,
                    "Нельзя оставить чаевые на эту сумму — это уведёт ваш баланс в минус.");
            return;
        }

        paymentService.addTip(sub.getId(), amount);
        auditService.logTip(user, sub.getService().getName(), amount, sub.getService().getCurrency());
        sessionManager.resetSession(user.getTelegramId());
        messageSender.send(chatId,
                "\u2615 Спасибо за чаевые! <b>" + amount + " " + sub.getService().getCurrency() + "</b> списано с баланса.",
                KeyboardBuilder.backToServiceKeyboard(sub.getService().getId()));
    }

    private void handleCreatorMessageInput(Long chatId, BotUser user, UserSession session, String text) {
        Subscription sub = subscriptionService
                .findByUserAndService(user.getId(), session.getServiceId())
                .orElse(null);
        if (sub == null) {
            sessionManager.resetSession(user.getTelegramId());
            return;
        }

        CreatorMessage msg = new CreatorMessage(sub, text);

        BotUser creator = sub.getService().getCreatedBy();
        if (creator.getTelegramId() != null) {
            messageSender.send(creator.getTelegramId(),
                    "\u2709\uFE0F Сообщение от " + user.getDisplayName()
                            + " (сервис: " + sub.getService().getName() + "):\n\n" + text);
            msg.setDelivered(true);
        }

        creatorMessageRepository.save(msg);
        auditService.logCreatorMessage(user, sub.getService().getName(), text);
        sessionManager.resetSession(user.getTelegramId());

        String confirmation = msg.getDelivered()
                ? "\u2705 Сообщение отправлено создателю!"
                : "\u2705 Сообщение сохранено. Создатель увидит его позже.";
        messageSender.send(chatId, confirmation,
                KeyboardBuilder.backToServiceKeyboard(sub.getService().getId()));
    }

    private void handleDeletePayment(Long chatId, Integer messageId, BotUser user, Long paymentId) {
        Payment payment = paymentRepository.findByIdWithDetails(paymentId).orElse(null);
        if (payment == null || payment.getDeleted()) return;

        Long serviceId = payment.getSubscription().getService().getId();
        auditService.logPaymentDelete(user, payment.getSubscription().getService().getName(),
                payment.getAmount(), payment.getCurrency(), payment.getCreatedAt());
        paymentService.softDeletePayment(paymentId);
        sendOrEdit(chatId, messageId,
                "\uD83D\uDDD1 Платёж удалён. Баланс пересчитан.",
                KeyboardBuilder.backToServiceKeyboard(serviceId));
    }

    private void handleDeleteTip(Long chatId, Integer messageId, BotUser user, Long tipId) {
        TipPayment tip = tipPaymentRepository.findByIdWithDetails(tipId).orElse(null);
        if (tip == null || tip.getDeleted()) return;

        Long serviceId = tip.getSubscription().getService().getId();
        auditService.logTip(user, tip.getSubscription().getService().getName(),
                tip.getAmount().negate(), tip.getSubscription().getService().getCurrency());
        paymentService.softDeleteTip(tipId);
        sendOrEdit(chatId, messageId,
                "\uD83D\uDDD1 Чаевые удалены. Баланс восстановлен.",
                KeyboardBuilder.backToServiceKeyboard(serviceId));
    }

    // --- Helpers ---

    private void sendOrEdit(Long chatId, Integer messageId, String text,
                            org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup keyboard) {
        if (messageId != null) {
            messageSender.editMessage(chatId, messageId, text, keyboard);
        } else {
            messageSender.send(chatId, text, keyboard);
        }
    }

    private void answerCallback(String callbackQueryId) {
        try {
            execute(AnswerCallbackQuery.builder().callbackQueryId(callbackQueryId).build());
        } catch (TelegramApiException e) {
            log.warn("Failed to answer callback query", e);
        }
    }

    private void registerCommands() {
        try {
            execute(SetMyCommands.builder()
                    .commands(List.of(
                            BotCommand.builder().command("start").description("Главное меню").build(),
                            BotCommand.builder().command("menu").description("Главное меню").build(),
                            BotCommand.builder().command("admin").description("Админ-панель").build()
                    ))
                    .build());
            log.info("Bot commands registered");
        } catch (TelegramApiException e) {
            log.warn("Failed to register bot commands", e);
        }
    }
}
