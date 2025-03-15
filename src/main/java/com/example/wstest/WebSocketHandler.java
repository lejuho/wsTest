package com.example.wstest;

import com.example.wstest.dto.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

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

        ChatMessage chatMessage = objectMapper.readValue(payload, ChatMessage.class);

        // Get the authenticated user from session attributes
        Map<String, Object> attributes = session.getAttributes();
        UserDetails userDetails = (UserDetails) attributes.get("user");

        // Set the sender as the authenticated username
        if (userDetails != null) {
            chatMessage.setSender(userDetails.getUsername());
            log.info("Authenticated user: {}", userDetails.getUsername());
        } else {
            log.warn("No authenticated user found in session");
            return; // Don't process messages from unauthenticated users
        }

        ChatRoom chatRoom = chatService.findRoomById(chatMessage.getRoomId());
        if (chatRoom != null) {
            chatRoom.handlerActions(session, chatMessage, chatService);
        } else {
            log.warn("Chat room not found: {}", chatMessage.getRoomId());
        }
    }
}