package com.example.wstest.auth;

import com.example.wstest.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
        String query = servletRequest.getServletRequest().getQueryString();

        log.info("웹소켓 연결 시도: {}", request.getURI());

        // CORS 관련 헤더 추가
        response.getHeaders().add("Access-Control-Allow-Origin", "*");
        response.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        response.getHeaders().add("Access-Control-Allow-Headers", "*");

        // 토큰이 없는 경우 public 채널로 연결 허용 (필요에 따라 구현)
        if (query == null || !query.contains("token=")) {
            log.warn("토큰이 없는 연결 시도");
            return true; // 토큰 검증 안하고 연결 허용 (필요에 따라 변경)
        }

        try {
            String token = extractToken(query);
            if (token != null && jwtTokenProvider.validateToken(token)) {
                String username = jwtTokenProvider.getUsername(token);
                attributes.put("username", username);
                log.info("인증된 사용자 연결: {}", username);
                return true;
            } else {
                log.warn("유효하지 않은 토큰: {}", token);
                return false;
            }
        } catch (Exception e) {
            log.error("웹소켓 인증 실패: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("핸드셰이크 후 예외 발생: {}", exception.getMessage());
        }
    }

    private String extractToken(String query) {
        String[] params = query.split("&");
        for (String param : params) {
            if (param.startsWith("token=")) {
                return param.substring(6); // "token=" 부분 제거
            }
        }
        return null;
    }
}