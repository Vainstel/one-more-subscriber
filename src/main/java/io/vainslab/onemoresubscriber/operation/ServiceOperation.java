package io.vainslab.onemoresubscriber.operation;

import io.vainslab.onemoresubscriber.entity.Subscription;

public interface ServiceOperation {

    String getCode();

    String getButtonLabel();

    default int getOrder() {
        return 50;
    }

    boolean isAvailable(Subscription subscription);

    void execute(OperationContext ctx);
}
