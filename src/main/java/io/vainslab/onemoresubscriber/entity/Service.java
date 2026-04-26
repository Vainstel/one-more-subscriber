package io.vainslab.onemoresubscriber.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "service")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = "password")
public class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(columnDefinition = "TEXT")
    private String rules;

    @Column(name = "monthly_cost", nullable = false)
    private BigDecimal monthlyCost;

    @Column(nullable = false)
    private String currency = "RUB";

    @Column(name = "max_members", nullable = false)
    private Integer maxMembers;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private BotUser createdBy;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "billing_active", nullable = false)
    private Boolean billingActive = true;

    private String password;

    @Column(name = "join_description", columnDefinition = "TEXT")
    private String joinDescription;

    @Column(name = "service_type", nullable = false)
    private String serviceType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
