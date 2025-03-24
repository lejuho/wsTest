package com.example.wstest;

import com.example.wstest.dto.ChatMessage;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Getter
public class ChatRoom {
    private String roomId;
    private String name;

    @JsonIgnore
    private Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Builder
    public ChatRoom(String roomId, String name) {
        this.roomId = roomId;
        this.name = name;
    }

    public void handlerActions(WebSocketSession session, ChatMessage chatMessage, ChatService chatService) {
        if (chatMessage.getType().equals(ChatMessage.MessageType.ENTER)) {
            sessions.add(session);
            chatMessage.setMessage(chatMessage.getSender() + "님이 입장했습니다.");

            // 사용자 입장 시 이전 메시지 전송 - 비동기 처리로 개선
            try {
                sendPreviousMessages(session, chatService);
            } catch (Exception e) {
                log.error("이전 메시지 전송 실패: {}", e.getMessage());
            }
        } else if (chatMessage.getType().equals(ChatMessage.MessageType.LEAVE)) {
            sessions.remove(session);
            chatMessage.setMessage(chatMessage.getSender() + "님이 퇴장했습니다.");
        }

        // 메시지 전송 처리를 별도로 분리
        sendMessageToAll(chatMessage, chatService);
    }

    // 이전 메시지 전송 - 비동기 처리
    private void sendPreviousMessages(WebSocketSession session, ChatService chatService) {
        new Thread(() -> {
            try {
                for (ChatMessage prevMsg : chatService.getPreviousMessages(roomId)) {
                    chatService.sendMessage(prevMsg);
                    // 너무 빠른 메시지 전송 방지를 위한 짧은 대기
                    Thread.sleep(20);
                }
            } catch (Exception e) {
                log.error("이전 메시지 비동기 전송 실패: {}", e.getMessage());
            }
        }).start();
    }

    // 모든 세션에 메시지 전송 - 동시성 처리 개선
    public void sendMessageToAll(ChatMessage message, ChatService chatService) {
        // ChatService의 sendMessage를 한 번만 호출하도록 수정
        chatService.sendMessage(message);
    }

}