package com.levelupjourney.microserviceapigateway.shared.config;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Order(0)
public class LoggingGlobalFilter implements GlobalFilter {

    private static final Logger logger = LoggerFactory.getLogger(LoggingGlobalFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var request = exchange.getRequest();
        String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";
        logger.info("➡️  Incoming request: [{}] {}", method, request.getURI());

        return chain.filter(exchange)
                .doOnSuccess(unused -> logResponse(exchange, method))
                .doOnError(error -> logError(exchange, method, error));
    }

    private void logResponse(ServerWebExchange exchange, String method) {
        var request = exchange.getRequest();
        HttpStatusCode status = exchange.getResponse().getStatusCode();
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        URI targetUri = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
        String routeId = route != null ? route.getId() : "unknown";

        logger.info("⬅️  Response: {} {} -> {} (proxied to: [{}] {})",
                method,
                request.getURI(),
                status != null ? status.value() : "n/a",
                routeId,
                targetUri != null ? targetUri : "n/a");
    }

    private void logError(ServerWebExchange exchange, String method, Throwable error) {
        var request = exchange.getRequest();
        URI targetUri = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String routeId = route != null ? route.getId() : "unknown";

        logger.error("❌ Error on {} {} (proxied to: [{}] {}): {}",
                method,
                request.getURI(),
                routeId,
                targetUri != null ? targetUri : "n/a",
                error.getMessage(),
                error);
    }
}
