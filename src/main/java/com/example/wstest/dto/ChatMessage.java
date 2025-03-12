package com.example.wstest.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class ChatMessage {

    public enum MessageType {
        ENTER, TALK, LEAVE, TYPING_START, TYPING_END, READ_RECEIPT
    }

    private MessageType type;
    private String roomId;
    private String sender;
    private String message;
    private LocalDateTime timestamp;
    private String messageId; // 메시지 고유 ID
    private Set<String> readBy; // 메시지를 읽은 사용자 목록
    private boolean isTyping; // 타이핑 상태

    public ChatMessage() {
        this.timestamp = LocalDateTime.now();
        this.messageId = java.util.UUID.randomUUID().toString();
        this.readBy = new HashSet<>();
    }

    // 메시지가 특정 사용자에 의해 읽힘 처리
    public void markAsReadBy(String userId) {
        if (this.readBy == null) {
            this.readBy = new HashSet<>();
        }
        this.readBy.add(userId);
    }

    // 읽음 상태 확인
    public boolean isReadBy(String userId) {
        return readBy != null && readBy.contains(userId);
    }
}
