// ChatMessageRepositoryImpl 구현체 생성 (예: 인메모리 구현)
package com.example.wstest.repository;

import com.example.wstest.dto.ChatMessage;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class ChatMessageRepositoryImpl implements ChatMessageRepository {
    // ConcurrentHashMap 사용
    private Map<String, List<ChatMessage>> messagesByRoomId = new ConcurrentHashMap<>();

    @Override
    public ChatMessage save(ChatMessage message) {
        // 동시성 문제를 방지하기 위해 synchronized 블록 사용
        synchronized (messagesByRoomId) {
            List<ChatMessage> messages = messagesByRoomId.getOrDefault(message.getRoomId(), new ArrayList<>());
            messages.add(message);
            messagesByRoomId.put(message.getRoomId(), messages);
            return message;
        }
    }

    @Override
    public List<ChatMessage> findByRoomId(String roomId) {
        // 결과를 반환할 때는 항상 새로운 리스트로 복사하여 반환
        return new ArrayList<>(messagesByRoomId.getOrDefault(roomId, new ArrayList<>()));
    }
}