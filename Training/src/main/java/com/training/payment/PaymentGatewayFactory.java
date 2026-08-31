package com.training.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PaymentGatewayFactory {

    private final Map<String, PaymentGateway> gatewayMap;
    private final String defaultProvider;

    public PaymentGatewayFactory(
            List<PaymentGateway> gateways,
            @Value("${app.payment.provider:dummy}") String defaultProvider) {
        this.gatewayMap = gateways.stream()
                .collect(Collectors.toMap(g -> g.getProviderName().toUpperCase(), g -> g));
        this.defaultProvider = defaultProvider.toUpperCase();
    }

    public PaymentGateway getGateway() {
        return getGateway(defaultProvider);
    }

    public PaymentGateway getGateway(String providerName) {
        String key = (providerName != null && !providerName.trim().isEmpty()) 
                ? providerName.trim().toUpperCase() 
                : defaultProvider;
        PaymentGateway gateway = gatewayMap.get(key);
        if (gateway == null) {
            gateway = gatewayMap.get("DUMMY");
        }
        if (gateway == null && !gatewayMap.isEmpty()) {
            gateway = gatewayMap.values().iterator().next();
        }
        if (gateway == null) {
            throw new IllegalStateException("No PaymentGateway implementation configured in system.");
        }
        return gateway;
    }
}
