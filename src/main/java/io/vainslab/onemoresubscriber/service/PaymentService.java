package io.vainslab.onemoresubscriber.service;

import io.vainslab.onemoresubscriber.config.BotProperties;
import io.vainslab.onemoresubscriber.entity.BalanceDeduction;
import io.vainslab.onemoresubscriber.entity.Payment;
import io.vainslab.onemoresubscriber.entity.Subscription;
import io.vainslab.onemoresubscriber.entity.TipPayment;
import io.vainslab.onemoresubscriber.repository.BalanceDeductionRepository;
import io.vainslab.onemoresubscriber.repository.PaymentRepository;
import io.vainslab.onemoresubscriber.repository.SubscriptionRepository;
import io.vainslab.onemoresubscriber.repository.TipPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TipPaymentRepository tipPaymentRepository;
    private final BalanceDeductionRepository balanceDeductionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final BalanceService balanceService;
    private final BotProperties botProperties;

    @Transactional
    public Payment addPayment(Long subscriptionId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        Subscription subscription = subscriptionRepository.findById(subscriptionId).orElseThrow();
        Payment payment = new Payment(subscription, amount, subscription.getService().getCurrency());
        payment = paymentRepository.save(payment);
        balanceService.addToBalance(subscription, amount);
        return payment;
    }

    @Transactional
    public void softDeletePayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        payment.setDeleted(true);
        payment.setDeletedAt(LocalDateTime.now());
        paymentRepository.save(payment);
        balanceService.subtractFromBalance(payment.getSubscription(), payment.getAmount());
    }

    public List<Payment> getPayments(Long subscriptionId) {
        return paymentRepository.findAllBySubscriptionIdAndDeletedFalseOrderByCreatedAtDesc(subscriptionId);
    }

    public List<Payment> getDeletablePayments(Long subscriptionId) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(botProperties.getPayment().getDeleteWindowHours());
        return paymentRepository.findAllBySubscriptionIdAndDeletedFalseAndCreatedAtAfterOrderByCreatedAtDesc(
                subscriptionId, cutoff);
    }

    @Transactional
    public TipPayment addTip(Long subscriptionId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        Subscription subscription = subscriptionRepository.findById(subscriptionId).orElseThrow();
        TipPayment tip = new TipPayment(subscription, amount);
        tip = tipPaymentRepository.save(tip);
        balanceService.subtractFromBalance(subscription, amount);
        return tip;
    }

    @Transactional
    public void softDeleteTip(Long tipId) {
        TipPayment tip = tipPaymentRepository.findById(tipId).orElseThrow();
        tip.setDeleted(true);
        tip.setDeletedAt(LocalDateTime.now());
        tipPaymentRepository.save(tip);
        balanceService.addToBalance(tip.getSubscription(), tip.getAmount());
    }

    public List<TipPayment> getTips(Long subscriptionId) {
        return tipPaymentRepository.findAllBySubscriptionIdAndDeletedFalseOrderByCreatedAtDesc(subscriptionId);
    }

    public List<TipPayment> getDeletableTips(Long subscriptionId) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(botProperties.getPayment().getDeleteWindowHours());
        return tipPaymentRepository.findAllBySubscriptionIdAndDeletedFalseAndCreatedAtAfterOrderByCreatedAtDesc(
                subscriptionId, cutoff);
    }

    public List<BalanceDeduction> getDeductions(Long subscriptionId) {
        return balanceDeductionRepository.findAllBySubscriptionIdOrderByCreatedAtDesc(subscriptionId);
    }

    public BigDecimal getAvailableForTip(Long subscriptionId) {
        Subscription sub = subscriptionRepository.findById(subscriptionId).orElseThrow();
        return sub.getBalance().max(BigDecimal.ZERO);
    }

    public boolean canTip(Long subscriptionId, BigDecimal tipAmount) {
        Subscription sub = subscriptionRepository.findById(subscriptionId).orElseThrow();
        return sub.getBalance().compareTo(tipAmount) >= 0;
    }
}
