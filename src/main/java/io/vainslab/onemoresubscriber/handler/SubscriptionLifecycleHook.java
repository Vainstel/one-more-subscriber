package io.vainslab.onemoresubscriber.handler;

import io.vainslab.onemoresubscriber.entity.Subscription;

/**
 * Optional hook executed on subscription lifecycle events.
 * Implementations are tied to a specific serviceType via ServiceHandler.
 */
public interface SubscriptionLifecycleHook {

    /**
     * Called when a user leaves or is kicked from a subscription.
     * Implementation should handle errors gracefully — a failure here
     * should not prevent the leave/kick from completing.
     */
    void onLeave(Subscription subscription);
}
