package io.vainslab.onemoresubscriber.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "bot")
@Getter
@Setter
public class BotProperties {

    private String token;
    private List<Long> adminIds = List.of();
    private Notification notification = new Notification();
    private PaymentConfig payment = new PaymentConfig();

    public boolean isAdmin(Long telegramId) {
        return adminIds.contains(telegramId);
    }

    @Getter
    @Setter
    public static class Notification {
        private Duration checkInterval = Duration.ofHours(1);
        private int reminderIntervalDays = 3;
    }

    @Getter
    @Setter
    public static class PaymentConfig {
        private int deleteWindowHours = 48;
    }
}
