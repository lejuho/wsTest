package com.example.wstest;

import com.example.wstest.dto.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final ChatService chatService;

    // 세션 저장소
    private Map<String, WebSocketSession> sessions = new HashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("웹소켓 연결 성공: {}", session.getId());
        sessions.put(session.getId(), session);

        // 연결 성공 메시지 전송
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("type", "CONNECTION");
            response.put("status", "SUCCESS");
            response.put("message", "웹소켓 연결이 성공적으로 설정되었습니다");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        } catch (IOException e) {
            log.error("연결 성공 메시지 전송 실패", e);
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            ChatMessage chatMessage = objectMapper.readValue(message.getPayload(), ChatMessage.class);
            chatService.sendMessage(chatMessage);
        } catch (Exception e) {
            log.error("메시지 처리 중 오류 발생", e);
            sendErrorMessage(session, "MESSAGE_ERROR", "메시지 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("웹소켓 연결 종료: {}, 상태: {}", session.getId(), status);
        sessions.remove(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("웹소켓 전송 오류: {}", exception.getMessage(), exception);
        sendErrorMessage(session, "TRANSPORT_ERROR", "웹소켓 통신 중 오류가 발생했습니다: " + exception.getMessage());
    }

    // 오류 메시지 전송 메서드
    private void sendErrorMessage(WebSocketSession session, String errorType, String errorMessage) {
        try {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("type", errorType);
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", errorMessage);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorResponse)));
        } catch (IOException e) {
            log.error("오류 메시지 전송 실패", e);
        }
    }
}