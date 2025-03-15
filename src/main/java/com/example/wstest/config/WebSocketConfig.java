package com.example.wstest.config;

import com.example.wstest.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

@Slf4j
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketHandler webSocketHandler;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/ws/chat")
                .addInterceptors(new AuthHandshakeInterceptor(jwtTokenProvider))
                .setAllowedOrigins("*");

        log.info("WebSocket handler registered at /ws/chat");
    }

    @Slf4j
    public static class AuthHandshakeInterceptor implements HandshakeInterceptor {

        private final JwtTokenProvider jwtTokenProvider;

        public AuthHandshakeInterceptor(JwtTokenProvider jwtTokenProvider) {
            this.jwtTokenProvider = jwtTokenProvider;
        }

        @Override
        public boolean beforeHandshake(org.springframework.http.server.ServerHttpRequest request,
                                       org.springframework.http.server.ServerHttpResponse response,
                                       org.springframework.web.socket.WebSocketHandler wsHandler,
                                       Map<String, Object> attributes) throws Exception {

            String uri = request.getURI().toString();
            log.info("WebSocket connection attempt URI: {}", uri);

            String token = extractToken(uri);
            log.info("Extracted token: {}", token != null ? "present" : "absent");

            if (token != null && jwtTokenProvider.validateToken(token)) {
                log.info("Token validated successfully");
                Authentication auth = jwtTokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(auth);
                attributes.put("user", auth.getPrincipal());
                return true;
            } else {
                log.warn("Token validation failed or token not present");
                return false; // 이 부분이 문제일 수 있습니다
            }
        }

        private String extractToken(String uri) {
            log.debug("Extracting token from URI: {}", uri);
            int tokenIndex = uri.indexOf("token=");
            if (tokenIndex != -1) {
                String token = uri.substring(tokenIndex + 6);
                int endIndex = token.indexOf("&");
                if (endIndex != -1) {
                    token = token.substring(0, endIndex);
                }
                return token;
            }
            return null;
        }

        @Override
        public void afterHandshake(org.springframework.http.server.ServerHttpRequest request,
                                   org.springframework.http.server.ServerHttpResponse response,
                                   org.springframework.web.socket.WebSocketHandler wsHandler,
                                   Exception exception) {
            if (exception != null) {
                log.error("Exception after handshake: {}", exception.getMessage());
            } else {
                log.info("Handshake completed successfully");
            }
        }
    }
}