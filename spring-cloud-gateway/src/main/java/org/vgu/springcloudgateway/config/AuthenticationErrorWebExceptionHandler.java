package org.vgu.springcloudgateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles JWT and authentication failures by returning 401 Unauthorized with a JSON body
 * instead of letting the default handler return 500.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthenticationErrorWebExceptionHandler implements WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationErrorWebExceptionHandler.class);
    private final ObjectMapper objectMapper;

    public AuthenticationErrorWebExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (!isAuthenticationOrJwtError(ex)) {
            return Mono.error(ex);
        }

        var response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        String message = resolveMessage(ex);
        log.debug("Returning 401 for authentication/JWT error: {}", message);

        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "status", 401,
                "error", "Unauthorized",
                "message", message,
                "path", exchange.getRequest().getPath().value()
        );

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            log.warn("Failed to write 401 response body", e);
            return Mono.error(ex);
        }
    }

    private static boolean isAuthenticationOrJwtError(Throwable ex) {
        if (ex == null) return false;
        if (ex instanceof AuthenticationServiceException) return true;
        if (ex instanceof JwtException) return true;
        return isAuthenticationOrJwtError(ex.getCause());
    }

    private static String resolveMessage(Throwable ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof JwtException jwtEx) {
            String msg = jwtEx.getMessage();
            if (msg != null && (msg.contains("expired") || msg.contains("Expired"))) {
                return "Token expired";
            }
            if (msg != null && msg.contains("invalid")) {
                return "Invalid token";
            }
        }
        return "Token expired or invalid";
    }
}
