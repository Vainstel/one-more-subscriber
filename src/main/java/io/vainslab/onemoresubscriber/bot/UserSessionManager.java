package io.vainslab.onemoresubscriber.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class UserSessionManager {

    private static final Duration SESSION_TTL = Duration.ofHours(1);

    private final ConcurrentHashMap<Long, UserSession> sessions = new ConcurrentHashMap<>();

    public UserSession getSession(Long telegramUserId) {
        UserSession session = sessions.computeIfAbsent(telegramUserId, k -> new UserSession());
        session.touch();
        return session;
    }

    public void resetSession(Long telegramUserId) {
        sessions.computeIfPresent(telegramUserId, (k, v) -> {
            v.reset();
            return v;
        });
    }

    @Scheduled(fixedRate = 600_000) // every 10 minutes
    public void cleanupExpiredSessions() {
        Instant cutoff = Instant.now().minus(SESSION_TTL);
        int before = sessions.size();
        sessions.entrySet().removeIf(e -> e.getValue().getLastAccessedAt().isBefore(cutoff));
        int removed = before - sessions.size();
        if (removed > 0) {
            log.debug("Cleaned up {} expired sessions", removed);
        }
    }
}
