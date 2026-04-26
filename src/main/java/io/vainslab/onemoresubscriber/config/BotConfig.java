package io.vainslab.onemoresubscriber.config;

import io.vainslab.onemoresubscriber.bot.AdminHandler;
import io.vainslab.onemoresubscriber.bot.SubscriptionBot;
import io.vainslab.onemoresubscriber.bot.UserSessionManager;
import io.vainslab.onemoresubscriber.handler.ServiceHandlerRegistry;
import io.vainslab.onemoresubscriber.repository.CreatorMessageRepository;
import io.vainslab.onemoresubscriber.repository.PaymentRepository;
import io.vainslab.onemoresubscriber.repository.ServiceRepository;
import io.vainslab.onemoresubscriber.repository.TipPaymentRepository;
import io.vainslab.onemoresubscriber.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class BotConfig {

    @Bean
    public SubscriptionBot subscriptionBot(BotProperties botProperties,
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
        return new SubscriptionBot(botProperties, botUserService, subscriptionService,
                paymentService, errorLoggingService, serviceRepository,
                paymentRepository, tipPaymentRepository, creatorMessageRepository,
                handlerRegistry, sessionManager, adminHandler, auditService);
    }

    @Bean
    public TelegramBotsApi telegramBotsApi(SubscriptionBot bot) throws TelegramApiException {
        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        api.registerBot(bot);
        log.info("Telegram bot registered and started");
        return api;
    }
}
