package io.vainslab.onemoresubscriber.operation;

import io.vainslab.onemoresubscriber.bot.KeyboardBuilder;
import io.vainslab.onemoresubscriber.entity.Subscription;
import io.vainslab.onemoresubscriber.repository.SubscriptionRepository;
import io.vainslab.onemoresubscriber.service.AwgApiClient;
import io.vainslab.onemoresubscriber.service.AwgApiClient.CreatePeerResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class NewKeyOperation implements ServiceOperation {

    private static final String META_VPN_IP = "vpn_client_ip";
    private static final long OLD_KEY_DELETE_DELAY_SECONDS = 10;

    private final AwgApiClient awgApiClient;
    private final SubscriptionRepository subscriptionRepository;

    @Override
    public String getCode() { return "newkey"; }

    @Override
    public int getOrder() { return 61; }

    @Override
    public String getButtonLabel() {
        return "🔑 Новый ключ";
    }

    @Override
    public boolean isAvailable(Subscription subscription) {
        return subscription != null && subscription.getActive() && awgApiClient.isConfigured();
    }

    @Override
    public void execute(OperationContext ctx) {
        Subscription sub = ctx.getSubscription();
        var keyboard = KeyboardBuilder.backToServiceKeyboard(ctx.getService().getId());

        String oldIp = (String) sub.getMeta().get(META_VPN_IP);

        CreatePeerResult result = awgApiClient.createPeer();
        if (result == null) {
            ctx.reply("❌ Не удалось создать ключ. Попробуйте позже.", keyboard);
            return;
        }

        sub.getMeta().put(META_VPN_IP, result.clientIp());
        subscriptionRepository.save(sub);

        log.info("Created VPN key ip={} for subscription={} user={}",
                result.clientIp(), sub.getId(), ctx.getBotUser().getTelegramId());

        ctx.getSender().send(ctx.getChatId(),
                "🔑 Ваш новый ключ ниже.\n"
                + "Старый ключ будет удалён.\n"
                + "Скопируйте новый и вставьте в приложение AmneziaVPN.");
        ctx.getSender().send(ctx.getChatId(), "<pre>" + result.vpnUri() + "</pre>");
        ctx.reply("✅ Ключ выдан.", keyboard);

        if (oldIp != null) {
            deleteOldKeyAsync(oldIp, sub.getId());
        }
    }

    @Async
    void deleteOldKeyAsync(String oldIp, Long subscriptionId) {
        try {
            TimeUnit.SECONDS.sleep(OLD_KEY_DELETE_DELAY_SECONDS);
            boolean deleted = awgApiClient.deletePeer(oldIp);
            if (deleted) {
                log.info("Deleted old VPN key ip={} for subscription={}", oldIp, subscriptionId);
            } else {
                log.warn("Skipped: failed to delete old VPN key ip={} for subscription={}", oldIp, subscriptionId);
            }
        } catch (Exception e) {
            log.warn("Skipped: error deleting old VPN key ip={} for subscription={}: {}",
                    oldIp, subscriptionId, e.getMessage());
        }
    }
}
