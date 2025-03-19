// ChatMessageRepository 인터페이스 생성
package com.example.wstest.repository;

import com.example.wstest.dto.ChatMessage;
import java.util.List;

public interface ChatMessageRepository {
    ChatMessage save(ChatMessage message);
    List<ChatMessage> findByRoomId(String roomId);
}