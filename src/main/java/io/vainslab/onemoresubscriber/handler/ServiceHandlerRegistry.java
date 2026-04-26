package io.vainslab.onemoresubscriber.handler;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ServiceHandlerRegistry {

    private final Map<String, ServiceHandler> handlers;
    private final ServiceHandler defaultHandler;

    public ServiceHandlerRegistry(List<ServiceHandler> handlerList) {
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(ServiceHandler::getServiceType, Function.identity()));
        this.defaultHandler = handlers.getOrDefault("DEFAULT", handlerList.getFirst());
    }

    public ServiceHandler getHandler(String serviceType) {
        return handlers.getOrDefault(serviceType, defaultHandler);
    }
}
