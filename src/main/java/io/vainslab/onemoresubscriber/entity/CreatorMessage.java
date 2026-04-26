package io.vainslab.onemoresubscriber.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "creator_message")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class CreatorMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(name = "message_text", nullable = false, columnDefinition = "TEXT")
    private String messageText;

    @Column(nullable = false)
    private Boolean delivered = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public CreatorMessage(Subscription subscription, String messageText) {
        this.subscription = subscription;
        this.messageText = messageText;
    }
}
