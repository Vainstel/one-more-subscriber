package io.vainslab.onemoresubscriber.operation;

import io.vainslab.onemoresubscriber.entity.Subscription;
import org.springframework.stereotype.Component;

@Component
public class BackOperation implements ServiceOperation {

    @Override
    public String getCode() { return "back"; }

    @Override
    public int getOrder() { return 100; }

    @Override
    public String getButtonLabel() {
        return "◀\uFE0F Назад";
    }

    @Override
    public boolean isAvailable(Subscription subscription) {
        return true;
    }

    @Override
    public void execute(OperationContext ctx) {
        // handled directly in UpdateRouter
    }
}
