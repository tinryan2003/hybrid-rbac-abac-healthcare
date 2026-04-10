package org.vgu.springcloudgateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;

/**
 * Global filter to log all incoming requests and outgoing responses
 * Useful for debugging and auditing
 */
@Slf4j
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        long startTime = System.currentTimeMillis();
        
        // Log request details (convert headers to map to get keys)
        var headerNames = new ArrayList<>(request.getHeaders().toSingleValueMap().keySet());
        
        log.info("Incoming Request: method={}, uri={}, headerNames={}", 
            request.getMethod(), 
            request.getURI(), 
            headerNames);
        
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            long duration = System.currentTimeMillis() - startTime;
            
            log.info("Outgoing Response: status={}, duration={}ms, path={}", 
                response.getStatusCode(), 
                duration, 
                request.getPath());
        }));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE; // Execute last for accurate timing
    }
}

