package io.vainslab.onemoresubscriber.repository;

import io.vainslab.onemoresubscriber.entity.BalanceDeduction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BalanceDeductionRepository extends JpaRepository<BalanceDeduction, Long> {

    List<BalanceDeduction> findAllBySubscriptionIdOrderByCreatedAtDesc(Long subscriptionId);
}
