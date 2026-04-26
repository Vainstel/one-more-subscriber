package io.vainslab.onemoresubscriber.service;

import io.vainslab.onemoresubscriber.entity.BotUser;
import io.vainslab.onemoresubscriber.entity.Service;
import io.vainslab.onemoresubscriber.entity.Subscription;
import io.vainslab.onemoresubscriber.repository.ServiceRepository;
import io.vainslab.onemoresubscriber.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final ServiceRepository serviceRepository;
    private final BalanceService balanceService;

    public Optional<Subscription> findByUserAndService(Long userId, Long serviceId) {
        return subscriptionRepository.findByUserAndServiceWithDetails(userId, serviceId);
    }

    public int countActiveMembers(Long serviceId) {
        return subscriptionRepository.countByServiceIdAndActiveTrue(serviceId);
    }

    @Transactional
    public Subscription join(BotUser user, io.vainslab.onemoresubscriber.entity.Service service) {
        Optional<Subscription> existing = subscriptionRepository
                .findByUserIdAndServiceId(user.getId(), service.getId());

        Subscription sub;
        if (existing.isPresent()) {
            sub = existing.get();
            sub.setActive(true);
            sub.setDeactivatedAt(null);
            sub.setLastDeductedAt(LocalDateTime.now());
            // balance is preserved from before — frozen on leave, resumed on rejoin
        } else {
            sub = new Subscription(user, service);
        }
        sub = subscriptionRepository.save(sub);

        balanceService.recalculateAllPaidUntil(service.getId());
        return sub;
    }

    public Optional<Subscription> findById(Long subscriptionId) {
        return subscriptionRepository.findByIdWithDetails(subscriptionId);
    }

    public List<Subscription> findAllByService(Long serviceId) {
        return subscriptionRepository.findAllByServiceIdWithUser(serviceId);
    }

    @Transactional
    public void leave(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId).orElseThrow();
        Long serviceId = subscription.getService().getId();
        subscription.setActive(false);
        subscription.setDeactivatedAt(LocalDateTime.now());
        subscriptionRepository.save(subscription);

        balanceService.recalculateAllPaidUntil(serviceId);
    }

    @Transactional
    public void setBillingActive(Long serviceId, boolean active) {
        Service service = serviceRepository.findById(serviceId).orElseThrow();
        service.setBillingActive(active);
        serviceRepository.save(service);

        if (active) {
            subscriptionRepository.resetLastDeductedAt(serviceId, LocalDateTime.now());
        }
    }

    @Transactional
    public void kick(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId).orElseThrow();
        Long serviceId = subscription.getService().getId();
        subscription.setActive(false);
        subscription.setDeactivatedAt(LocalDateTime.now());
        if (subscription.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            subscription.setBalance(BigDecimal.ZERO);
        }
        subscriptionRepository.save(subscription);

        balanceService.recalculateAllPaidUntil(serviceId);
    }
}
