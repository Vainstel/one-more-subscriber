package io.vainslab.onemoresubscriber.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_log")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt = LocalDateTime.now();

    @Column(nullable = false)
    private String type;

    public NotificationLog(Subscription subscription, String type) {
        this.subscription = subscription;
        this.type = type;
    }
}
