package io.vainslab.onemoresubscriber.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "subscription", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "service_id"}))
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private BotUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();

    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt;

    @Column(name = "paid_until")
    private LocalDateTime paidUntil;

    @Column(nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "last_deducted_at")
    private LocalDateTime lastDeductedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> meta = new HashMap<>();

    public Subscription(BotUser user, Service service) {
        this.user = user;
        this.service = service;
        this.paidUntil = this.joinedAt;
        this.balance = BigDecimal.ZERO;
        this.lastDeductedAt = this.joinedAt;
    }

    public boolean isInGracePeriod() {
        return joinedAt.plusHours(1).isAfter(LocalDateTime.now());
    }

    public boolean isInDebt() {
        if (isInGracePeriod()) return false;
        return balance.compareTo(BigDecimal.ZERO) < 0;
    }
}
