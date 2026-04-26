package io.vainslab.onemoresubscriber.handler;

import io.vainslab.onemoresubscriber.entity.Subscription;
import io.vainslab.onemoresubscriber.repository.SubscriptionRepository;
import io.vainslab.onemoresubscriber.service.AwgApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class VpnLifecycleHook implements SubscriptionLifecycleHook {

    private static final String META_VPN_IP = "vpn_client_ip";

    private final AwgApiClient awgApiClient;
    private final SubscriptionRepository subscriptionRepository;

    @Override
    public void onLeave(Subscription subscription) {
        String clientIp = (String) subscription.getMeta().get(META_VPN_IP);
        if (clientIp == null) return;

        boolean deleted = awgApiClient.deletePeer(clientIp);
        if (deleted) {
            subscription.getMeta().remove(META_VPN_IP);
            subscriptionRepository.save(subscription);
            log.info("Deleted VPN key ip={} on leave for subscription={}", clientIp, subscription.getId());
        } else {
            log.warn("Failed to delete VPN key ip={} on leave for subscription={}", clientIp, subscription.getId());
        }
    }
}
