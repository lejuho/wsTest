package com.example.wstest;

import com.example.wstest.dto.ChatMessage;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Getter;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashSet;
import java.util.Set;


@Getter
public class ChatRoom {

    private final String roomId;
    private final String name;

    @JsonIgnore
    private final Set<WebSocketSession> sessions = new HashSet<>();

    @Builder //객체 생성에서 주입하는 것에 대한 방식 - Builder Pattern
    public ChatRoom(String roomId, String name) {
        this.roomId = roomId;
        this.name = name;
    }

    public void handlerActions(WebSocketSession session, ChatMessage chatMessage, ChatService chatService) {
        if (chatMessage.getType().equals(ChatMessage.MessageType.ENTER)) {
            //방에 처음 들어왔을때
            sessions.add(session);
            chatMessage.setMessage(chatMessage.getSender() + "님이 입장했습니다.");
        }


        // Store message in Redis
        try {
            chatService.cacheMessage(roomId, chatMessage);
        } catch (Exception e) {
            // Redis 에러 로깅
            System.err.println("Redis 캐싱 오류: " + e.getMessage());
        }

        // Send message to Kafka
        try {
            chatService.sendMessageToKafka("chat-messages", chatMessage);
        } catch (Exception e) {
            // Kafka 에러 로깅
            System.err.println("Kafka 메시지 전송 오류: " + e.getMessage());
        }

        sendMessage(chatMessage, chatService);
        //메세지 전송
    }

    public <T> void sendMessage(T message, ChatService chatService) {
        sessions.parallelStream()
                .forEach(session -> chatService.sendMessage(session, message));
        //채팅방에 입장해 있는 모든 클라이언트에게 메세지 전송
    }
}
