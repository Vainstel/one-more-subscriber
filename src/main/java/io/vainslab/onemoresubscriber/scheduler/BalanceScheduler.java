package io.vainslab.onemoresubscriber.scheduler;

import io.vainslab.onemoresubscriber.entity.Subscription;
import io.vainslab.onemoresubscriber.repository.SubscriptionRepository;
import io.vainslab.onemoresubscriber.service.BalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BalanceScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final BalanceService balanceService;

    @Scheduled(cron = "0 0 3 * * *")
    public void performDailyDeductions() {
        List<Subscription> active = subscriptionRepository.findAllActiveForDeduction();
        log.info("Starting daily deduction for {} subscriptions", active.size());

        for (Subscription sub : active) {
            try {
                if (sub.isInGracePeriod()) continue;

                BigDecimal dailyRate = balanceService.getDailyRate(sub);
                if (dailyRate.compareTo(BigDecimal.ZERO) == 0) continue;

                balanceService.deductDaily(sub, dailyRate);
            } catch (Exception e) {
                log.error("Failed to deduct for subscription {}", sub.getId(), e);
            }
        }

        log.info("Daily deduction completed");
    }
}
