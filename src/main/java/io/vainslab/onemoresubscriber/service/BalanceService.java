package io.vainslab.onemoresubscriber.service;

import io.vainslab.onemoresubscriber.entity.BalanceDeduction;
import io.vainslab.onemoresubscriber.entity.Subscription;
import io.vainslab.onemoresubscriber.repository.BalanceDeductionRepository;
import io.vainslab.onemoresubscriber.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BalanceService {

    private final SubscriptionRepository subscriptionRepository;
    private final BalanceDeductionRepository balanceDeductionRepository;

    @Transactional
    public void addToBalance(Subscription subscription, BigDecimal amount) {
        subscription.setBalance(subscription.getBalance().add(amount));
        updatePaidUntil(subscription);
        subscriptionRepository.save(subscription);
    }

    @Transactional
    public void subtractFromBalance(Subscription subscription, BigDecimal amount) {
        subscription.setBalance(subscription.getBalance().subtract(amount));
        updatePaidUntil(subscription);
        subscriptionRepository.save(subscription);
    }

    @Transactional
    public void deductDaily(Subscription subscription, BigDecimal dailyRate) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastDeducted = subscription.getLastDeductedAt();
        if (lastDeducted == null) {
            lastDeducted = subscription.getJoinedAt();
        }

        long daysMissed = ChronoUnit.DAYS.between(lastDeducted.toLocalDate(), now.toLocalDate());
        if (daysMissed <= 0) return;

        BigDecimal totalDeduction = dailyRate.multiply(BigDecimal.valueOf(daysMissed));
        subscription.setBalance(subscription.getBalance().subtract(totalDeduction));
        subscription.setLastDeductedAt(now);
        updatePaidUntil(subscription);
        subscriptionRepository.save(subscription);

        int memberCount = subscriptionRepository.countByServiceIdAndActiveTrue(subscription.getService().getId());
        balanceDeductionRepository.save(new BalanceDeduction(subscription, totalDeduction, (int) daysMissed, memberCount));
    }

    public void updatePaidUntil(Subscription subscription) {
        BigDecimal dailyRate = getDailyRate(subscription);
        if (dailyRate.compareTo(BigDecimal.ZERO) == 0) return;

        long daysRemaining = subscription.getBalance()
                .divide(dailyRate, 0, RoundingMode.FLOOR).longValue();
        subscription.setPaidUntil(LocalDateTime.now().plusDays(Math.max(0, daysRemaining)));
    }

    @Transactional
    public void recalculateAllPaidUntil(Long serviceId) {
        List<Subscription> subs = subscriptionRepository.findAllActiveByServiceIdWithService(serviceId);
        for (Subscription sub : subs) {
            updatePaidUntil(sub);
            subscriptionRepository.save(sub);
        }
    }

    public BigDecimal getDailyRate(Subscription subscription) {
        int memberCount = subscriptionRepository.countByServiceIdAndActiveTrue(
                subscription.getService().getId());
        if (memberCount == 0) return BigDecimal.ZERO;

        return subscription.getService().getMonthlyCost()
                .divide(BigDecimal.valueOf((long) memberCount * 30), 10, RoundingMode.HALF_UP);
    }
}
