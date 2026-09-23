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
				.route("category-route", r -> r.path("/api/greeting/**", "/api/greeting")
						.uri("lb://category-service"))
				.route("product-route", r -> r.path("/api/products/**")
						.uri("lb://product-service"))
				.route("order-route", r -> r.path("/api/orders/**")
						.uri("lb://order-service"))

				// Routes use service name for prefix (for Postman test)
				.route("category-service-route",
						r -> r.path("/category-service/**")
						.filters(f -> f.stripPrefix(1))
						.uri("lb://category-service"))
				.route("product-service-route",
						r -> r.path("/product-service/**")
						.filters(f -> f.stripPrefix(1))
						.uri("lb://product-service"))
				.route("order-service-route",
						r -> r.path("/order-service/**")
						.filters(f -> f.stripPrefix(1))
						.uri("lb://order-service"))
				.build();
	}
}
