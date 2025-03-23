package com.example.wstest;

import com.example.wstest.dto.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Date;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final ChatService chatService;

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("Received message: {}", payload);

        // Check authentication first
        Map<String, Object> attributes = session.getAttributes();
        Boolean authenticated = (Boolean) attributes.get("authenticated");
        if (authenticated != null && !authenticated) {
            sendErrorMessage(session, "인증에 실패했습니다. 다시 로그인해주세요.");
            return;
        }

        try {
            ChatMessage chatMessage = objectMapper.readValue(payload, ChatMessage.class);

            // Get authenticated user
            UserDetails userDetails = (UserDetails) attributes.get("user");

            if (userDetails != null) {
                chatMessage.setSender(userDetails.getUsername());
                log.info("Authenticated user: {}", userDetails.getUsername());

                // Set timestamp if not present
                if (chatMessage.getTimestamp() == null) {
                    chatMessage.setTimestamp(new Date());
                }

                ChatRoom chatRoom = chatService.findRoomById(chatMessage.getRoomId());
                if (chatRoom != null) {
                    chatRoom.handlerActions(session, chatMessage, chatService);
                } else {
                    log.warn("Chat room not found: {}", chatMessage.getRoomId());
                    sendErrorMessage(session, "존재하지 않는 채팅방입니다.");
                }
            } else {
                log.warn("No authenticated user found in session");
                sendErrorMessage(session, "인증된 사용자가 아닙니다.");
            }
        } catch (Exception e) {
            log.error("메시지 처리 중 오류 발생: {}", e.getMessage(), e);
            sendErrorMessage(session, "메시지 처리 중 오류가 발생했습니다.");
        }
    }

    private void sendErrorMessage(WebSocketSession session, String errorMessage) {
        try {
            ChatMessage errorChatMessage = new ChatMessage();
            errorChatMessage.setType(ChatMessage.MessageType.TALK);
            errorChatMessage.setSender("System");
            errorChatMessage.setMessage(errorMessage);
            errorChatMessage.setTimestamp(new Date());

            String payload = objectMapper.writeValueAsString(errorChatMessage);
            session.sendMessage(new TextMessage(payload));
        } catch (Exception e) {
            log.error("에러 메시지 전송 실패: {}", e.getMessage());
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket connection established: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket connection closed: {}, status: {}", session.getId(), status);

        // 모든 채팅방에서 세션 제거
        for (ChatRoom room : chatService.findAllRoom()) {
            room.getSessions().remove(session);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error: {}, error: {}", session.getId(), exception.getMessage());
    }
}