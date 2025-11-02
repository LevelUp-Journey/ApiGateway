package com.levelupjourney.microserviceapigateway.shared.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Order(0)
public class LoggingGlobalFilter implements GlobalFilter {

    private static final Logger logger = LoggerFactory.getLogger(LoggingGlobalFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().toString();
        String method = exchange.getRequest().getMethod().toString();
        String originalUri = exchange.getRequest().getURI().toString();

        logger.info("➡️  Incoming request: [{}] {}", method, originalUri);

        return chain.filter(exchange)
                .then(Mono.fromRunnable(() -> {
                    String routeId = exchange.getAttributeOrDefault("org.springframework.cloud.gateway.support.ServerWebExchangeUtils.gatewayRouteId", "unknown");
                    String targetUri = exchange.getAttributeOrDefault("org.springframework.cloud.gateway.support.ServerWebExchangeUtils.gatewayRequestUrl", "unknown").toString();
                    int statusCode = exchange.getResponse().getStatusCode() != null
                            ? exchange.getResponse().getStatusCode().value()
                            : 0;

                    logger.info("⬅️  Routed to [{}] → {} (status: {})", routeId, targetUri, statusCode);
                }));
    }
}
