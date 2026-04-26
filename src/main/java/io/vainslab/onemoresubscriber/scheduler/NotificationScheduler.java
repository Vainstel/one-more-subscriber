package io.vainslab.onemoresubscriber.scheduler;

import io.vainslab.onemoresubscriber.bot.BotMessageSender;
import io.vainslab.onemoresubscriber.bot.SubscriptionBot;
import io.vainslab.onemoresubscriber.entity.Service;
import io.vainslab.onemoresubscriber.entity.Subscription;
import io.vainslab.onemoresubscriber.repository.ServiceRepository;
import io.vainslab.onemoresubscriber.service.NotificationService;
import io.vainslab.onemoresubscriber.service.ServiceReportBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final NotificationService notificationService;
    private final ServiceReportBuilder reportBuilder;
    private final ServiceRepository serviceRepository;
    private final SubscriptionBot bot;

    @Scheduled(fixedDelayString = "${bot.notification.check-interval}")
    public void checkNotifications() {
        BotMessageSender sender = new BotMessageSender(bot);
        checkOverdue(sender);
        checkExpiringSoon(sender);
    }

    // Every Monday at 10:00
    @Scheduled(cron = "0 0 10 * * MON")
    public void sendWeeklyReports() {
        BotMessageSender sender = new BotMessageSender(bot);
        LocalDateTime since = LocalDateTime.now().minusWeeks(1);
        String periodLabel = since.format(FMT) + " — " + LocalDateTime.now().format(FMT);

        sendReportsToCreators(sender, since, "Неделя: " + periodLabel);
        log.info("Weekly reports sent");
    }

    // 1st of every month at 10:00
    @Scheduled(cron = "0 0 10 1 * *")
    public void sendMonthlyReports() {
        BotMessageSender sender = new BotMessageSender(bot);
        LocalDateTime since = LocalDateTime.now().minusMonths(1);
        String periodLabel = since.format(FMT) + " — " + LocalDateTime.now().format(FMT);

        sendReportsToCreators(sender, since, "Месяц: " + periodLabel);
        log.info("Monthly reports sent");
    }

    private void sendReportsToCreators(BotMessageSender sender, LocalDateTime since, String periodLabel) {
        List<Service> services = serviceRepository.findAllActiveWithCreator();
        for (Service service : services) {
            String report = reportBuilder.buildReport(service, since, periodLabel);
            Long creatorTelegramId = service.getCreatedBy().getTelegramId();
            sender.send(creatorTelegramId, report);
            log.info("Sent report for service={} to creator={}", service.getName(), creatorTelegramId);
        }
    }

    private void checkOverdue(BotMessageSender sender) {
        List<Subscription> overdue = notificationService.findOverdueSubscriptions();
        log.debug("Found {} overdue subscriptions", overdue.size());

        for (Subscription sub : overdue) {
            if (!notificationService.shouldNotifyOverdue(sub)) continue;

            long daysOverdue = ChronoUnit.DAYS.between(sub.getPaidUntil().toLocalDate(), LocalDate.now());
            String text = "⚠️ Напоминание: оплата по сервису <b>" + sub.getService().getName()
                    + "</b> закончилась " + sub.getPaidUntil().format(FMT)
                    + " (просрочка: " + daysOverdue + " дн.).\n"
                    + "Пожалуйста, внесите платёж.";

            sender.send(sub.getUser().getTelegramId(), text);
            notificationService.markNotifiedOverdue(sub);
            log.info("Sent overdue reminder to user={} for service={}",
                    sub.getUser().getTelegramId(), sub.getService().getName());
        }
    }

    private void checkExpiringSoon(BotMessageSender sender) {
        List<Subscription> expiring = notificationService.findExpiringSoon();
        log.debug("Found {} expiring subscriptions", expiring.size());

        for (Subscription sub : expiring) {
            if (!notificationService.shouldNotifyExpiring(sub)) continue;

            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), sub.getPaidUntil().toLocalDate());
            String text = "\uD83D\uDCC5 Через " + daysLeft + " дн. заканчивается оплата по сервису <b>"
                    + sub.getService().getName() + "</b> (до "
                    + sub.getPaidUntil().format(FMT) + ").\n"
                    + "Рекомендуем внести платёж заранее.";

            sender.send(sub.getUser().getTelegramId(), text);
            notificationService.markNotifiedExpiring(sub);
            log.info("Sent expiring-soon reminder to user={} for service={}",
                    sub.getUser().getTelegramId(), sub.getService().getName());
        }
    }
}
