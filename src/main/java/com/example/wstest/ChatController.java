package com.example.wstest;

import com.example.wstest.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/chat")
public class ChatController {
    private final ChatService chatService;

    @PostMapping
    public ChatRoom createRoom(@RequestBody String name) {
        return chatService.createRoom(name);
    }

    @GetMapping
    public List<ChatRoom> findAllRoom() {
        return chatService.findAllRoom();
    }

    // 메시지 읽음 상태 업데이트 엔드포인트
    @PostMapping("/messages/{messageId}/read")
    public ResponseEntity<?> markMessageAsRead(
            @PathVariable String messageId,
            @RequestBody Map<String, String> payload) {

        String userId = payload.get("userId");
        if (userId == null || userId.isEmpty()) {
            return ResponseEntity.badRequest().body("userId is required");
        }

        chatService.markMessageAsRead(messageId, userId);
        return ResponseEntity.ok().build();
    }

    // 타이핑 상태 업데이트 엔드포인트
    @PostMapping("/rooms/{roomId}/typing")
    public ResponseEntity<?> updateTypingStatus(
            @PathVariable String roomId,
            @RequestBody Map<String, Object> payload) {

        String userId = (String) payload.get("userId");
        Boolean isTyping = (Boolean) payload.get("isTyping");

        if (userId == null || isTyping == null) {
            return ResponseEntity.badRequest().body("userId and isTyping are required");
        }

        chatService.updateTypingStatus(roomId, userId, isTyping);
        return ResponseEntity.ok().build();
    }

    // 채팅 히스토리 조회 엔드포인트
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<ChatMessage>> getChatHistory(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "20") int limit) {

        List<ChatMessage> messages = chatService.getChatHistory(roomId, limit);
        return ResponseEntity.ok(messages);
    }
}
