package io.vainslab.onemoresubscriber.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "bot_user")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "telegramId")
public class BotUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telegram_id", nullable = false, unique = true)
    private Long telegramId;

    private String username;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private LocalDateTime registeredAt = LocalDateTime.now();

    public BotUser(Long telegramId, String firstName, String lastName, String username) {
        this.telegramId = telegramId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
    }

    public String getDisplayName() {
        return username != null ? "@" + username : firstName;
    }
}
