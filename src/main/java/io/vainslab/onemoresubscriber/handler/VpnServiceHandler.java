package io.vainslab.onemoresubscriber.handler;

import io.vainslab.onemoresubscriber.operation.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VpnServiceHandler extends AbstractServiceHandler {

    private final List<ServiceOperation> customOperations;
    private final SubscriptionLifecycleHook lifecycleHook;

    public VpnServiceHandler(JoinOperation join, LeaveOperation leave,
                             MakePaymentOperation pay, MyPaymentsOperation myPay,
                             DeletePaymentOperation delPay, TipOperation tip,
                             MessageCreatorOperation msg, BackOperation back,
                             NewKeyOperation newKey,
                             VpnLifecycleHook vpnLifecycleHook) {
        super(List.of(join, leave, pay, myPay, delPay, tip, msg, back));
        this.customOperations = List.of(newKey);
        this.lifecycleHook = vpnLifecycleHook;
    }

    @Override
    public String getServiceType() {
        return "VPN";
    }

    @Override
    public List<ServiceOperation> getCustomOperations() {
        return customOperations;
    }

    @Override
    public SubscriptionLifecycleHook getLifecycleHook() {
        return lifecycleHook;
    }
}
