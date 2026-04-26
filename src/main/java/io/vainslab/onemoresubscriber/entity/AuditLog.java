package io.vainslab.onemoresubscriber.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private BotUser user;

    private String username;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String action;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public AuditLog(BotUser user, String action) {
        this.user = user;
        this.username = user != null ? user.getDisplayName() : null;
        this.action = action;
    }
}
