package io.vainslab.onemoresubscriber.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "balance_deduction")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class BalanceDeduction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private Integer days;

    @Column(name = "member_count", nullable = false)
    private Integer memberCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public BalanceDeduction(Subscription subscription, BigDecimal amount, int days, int memberCount) {
        this.subscription = subscription;
        this.amount = amount;
        this.days = days;
        this.memberCount = memberCount;
    }
}
