package com.example.wstest;

import com.example.wstest.dto.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final ChatService chatService;

    // 세션 ID와 사용자 ID 매핑
    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("Received message: {}", payload);

        ChatMessage chatMessage = objectMapper.readValue(payload, ChatMessage.class);

        // 사용자 세션 매핑 저장
        if (chatMessage.getType() == ChatMessage.MessageType.ENTER) {
            sessionUserMap.put(session.getId(), chatMessage.getSender());
        }

        // 메시지 타입에 따른 처리
        switch (chatMessage.getType()) {
            case TALK:
                // 일반 메시지 처리
                ChatRoom chatRoom = chatService.findRoomById(chatMessage.getRoomId());
                chatRoom.handlerActions(session, chatMessage, chatService);
                break;

            case READ_RECEIPT:
                // 읽음 상태 업데이트
                chatService.markMessageAsRead(chatMessage.getMessageId(), chatMessage.getSender());
                break;

            case TYPING_START:
            case TYPING_END:
                // 타이핑 상태 업데이트
                boolean isTyping = chatMessage.getType() == ChatMessage.MessageType.TYPING_START;
                chatService.updateTypingStatus(chatMessage.getRoomId(), chatMessage.getSender(), isTyping);
                break;

            default:
                // ENTER, LEAVE 등 기존 메시지 처리
                ChatRoom room = chatService.findRoomById(chatMessage.getRoomId());
                room.handlerActions(session, chatMessage, chatService);
                break;
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        // 세션이 종료되면 해당 사용자의 타이핑 상태를 종료로 변경
        String userId = sessionUserMap.get(session.getId());
        if (userId != null) {
            // 사용자가 속한 모든 방에서 타이핑 상태 종료 처리
            // 실제 구현에서는 사용자가 속한 방 목록을 가져와서 각 방에 대해 처리해야 함
            for (ChatRoom room : chatService.findAllRoom()) {
                chatService.updateTypingStatus(room.getRoomId(), userId, false);
            }

            // 세션-사용자 매핑 제거
            sessionUserMap.remove(session.getId());
        }

        super.afterConnectionClosed(session, status);
    }
}