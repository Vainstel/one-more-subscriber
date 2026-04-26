package io.vainslab.onemoresubscriber.handler;

import io.vainslab.onemoresubscriber.entity.Subscription;
import io.vainslab.onemoresubscriber.operation.ServiceOperation;

import java.util.List;

public interface ServiceHandler {

    String getServiceType();

    List<ServiceOperation> getDefaultOperations();

    List<ServiceOperation> getCustomOperations();

    default List<ServiceOperation> getAllOperations() {
        List<ServiceOperation> all = new java.util.ArrayList<>(getDefaultOperations());
        all.addAll(getCustomOperations());
        return all;
    }

    default List<ServiceOperation> getAvailableOperations(Subscription subscription) {
        return getAllOperations().stream()
                .filter(op -> op.isAvailable(subscription))
                .sorted(java.util.Comparator.comparingInt(ServiceOperation::getOrder))
                .toList();
    }

    String buildServiceInfoMessage(io.vainslab.onemoresubscriber.entity.Service service,
                                   Subscription subscription,
                                   int memberCount,
                                   java.math.BigDecimal userBalance);
}
