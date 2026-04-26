package io.vainslab.onemoresubscriber.operation;

import io.vainslab.onemoresubscriber.bot.KeyboardBuilder;
import io.vainslab.onemoresubscriber.entity.Subscription;
import io.vainslab.onemoresubscriber.repository.SubscriptionRepository;
import io.vainslab.onemoresubscriber.service.AwgApiClient;
import io.vainslab.onemoresubscriber.service.AwgApiClient.CreatePeerResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NewKeyOperation implements ServiceOperation {

    private static final String META_VPN_IP = "vpn_client_ip";

    private final AwgApiClient awgApiClient;
    private final SubscriptionRepository subscriptionRepository;

    @Override
    public String getCode() { return "newkey"; }

    @Override
    public int getOrder() { return 61; }

    @Override
    public String getButtonLabel() {
        return "\uD83D\uDD11 Новый ключ";
    }

    @Override
    public boolean isAvailable(Subscription subscription) {
        return subscription != null && subscription.getActive() && awgApiClient.isConfigured();
    }

    @Override
    public void execute(OperationContext ctx) {
        Subscription sub = ctx.getSubscription();
        var keyboard = KeyboardBuilder.backToServiceKeyboard(ctx.getService().getId());

        // Delete old key if exists
        String oldIp = (String) sub.getMeta().get(META_VPN_IP);
        if (oldIp != null) {
            boolean deleted = awgApiClient.deletePeer(oldIp);
            if (!deleted) {
                log.warn("Failed to delete old VPN key ip={} for subscription={}", oldIp, sub.getId());
                ctx.reply("❌ Не удалось удалить старый ключ. Попробуйте позже.", keyboard);
                return;
            }
            log.info("Deleted old VPN key ip={} for subscription={}", oldIp, sub.getId());
        }

        // Create new key
        CreatePeerResult result = awgApiClient.createPeer();
        if (result == null) {
            ctx.reply("❌ Не удалось создать ключ. Попробуйте позже.", keyboard);
            return;
        }

        // Save IP to meta
        sub.getMeta().put(META_VPN_IP, result.clientIp());
        subscriptionRepository.save(sub);

        log.info("Created VPN key ip={} for subscription={} user={}",
                result.clientIp(), sub.getId(), ctx.getBotUser().getTelegramId());

        // Send vpn:// URI as a separate message (so user can copy it)
        ctx.getSender().send(ctx.getChatId(),
                "🔑 Ваш VPN-ключ:\n\n```\n" + result.vpnUri() + "\n```\n\n"
                + "Скопируйте и вставьте в приложение AmneziaVPN.");
        ctx.reply("✅ Ключ выдан.", keyboard);
    }
}
