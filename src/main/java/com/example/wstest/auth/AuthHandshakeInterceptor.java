package com.example.wstest.auth;

import com.example.wstest.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
public class AuthHandshakeInterceptor implements HandshakeInterceptor {

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

        // Better token extraction
        String token = null;
        if (request.getURI().getQuery() != null) {
            String query = request.getURI().getQuery();
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                if (idx > 0 && pair.substring(0, idx).equals("token")) {
                    token = pair.substring(idx + 1);
                    break;
                }
            }
        }

        log.info("Extracted token: {}", token != null ? "present" : "absent");

        if (token != null && jwtTokenProvider.validateToken(token)) {
            log.info("Token validated successfully");
            Authentication auth = jwtTokenProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(auth);
            attributes.put("user", auth.getPrincipal());
            return true;
        } else {
            log.warn("Token validation failed or token not present");
            // Return true to allow connection, but handle authentication in the WebSocketHandler
            // This approach allows you to send error messages to the client
            attributes.put("authenticated", false);
            return true;
        }
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
