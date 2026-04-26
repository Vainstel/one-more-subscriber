package io.vainslab.onemoresubscriber.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "error_log")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class ErrorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private BotUser user;

    @Column(nullable = false)
    private String action;

    @Column(name = "exception_type", nullable = false)
    private String exceptionType;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
