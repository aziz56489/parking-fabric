package com.parking.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Shows the prices read from the config server (config-server/.../config/parking-service.yml).
 * After editing that file, POST /actuator/refresh (admin token) reloads them without a restart.
 */
@RestController
@RefreshScope
public class PricingController {

    @Value("${parking.price-per-hour}")
    private BigDecimal pricePerHour;

    @Value("${parking.currency}")
    private String currency;

    @Value("${parking.pricing.source}")
    private String source;

    @GetMapping("/api/pricing")
    public Map<String, Object> pricing() {
        return Map.of("pricePerHour", pricePerHour, "currency", currency, "source", source);
    }
}
