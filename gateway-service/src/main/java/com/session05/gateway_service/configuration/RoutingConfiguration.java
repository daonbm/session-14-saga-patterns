package com.session05.gateway_service.configuration;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RoutingConfiguration {

    @Bean
	public RouteLocator routes(RouteLocatorBuilder builder) {
		return builder.routes()
				.route("order-route", r -> r.path("/api/orders/**")
						.uri("lb://order-service"))
				.route("payment-health-route", r -> r.path("/payment-service/**")
						.filters(f -> f.stripPrefix(1))
						.uri("lb://payment-service"))
				.route("inventory-health-route", r -> r.path("/inventory-service/**")
						.filters(f -> f.stripPrefix(1))
						.uri("lb://inventory-service"))
				.route("order-service-route", r -> r.path("/order-service/**")
						.filters(f -> f.stripPrefix(1))
						.uri("lb://order-service"))
				.build();
	}
}
