package io.vainslab.onemoresubscriber.bot;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class UserSession {

    private UserState state = UserState.IDLE;
    private Long serviceId;
    private Long subscriptionId;
    private Instant lastAccessedAt = Instant.now();

    public void touch() {
        this.lastAccessedAt = Instant.now();
    }

    public void reset() {
        this.state = UserState.IDLE;
        this.serviceId = null;
        this.subscriptionId = null;
        touch();
    }

    public enum UserState {
        IDLE,
        AWAITING_SERVICE_PASSWORD,
        AWAITING_PAYMENT_AMOUNT,
        AWAITING_TIP_AMOUNT,
        AWAITING_CREATOR_MESSAGE
    }
}
