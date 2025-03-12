package com.example.wstest;

import com.example.wstest.dto.ChatMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
@Service
public class ChatService {

    private final ObjectMapper objectMapper;
    private final ChatRoomRepository chatRoomRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private Map<String, ChatRoom> chatRooms;
    // 메시지 ID로 메시지를 조회하기 위한 맵
    private Map<String, ChatMessage> messageCache = new ConcurrentHashMap<>();
    // 사용자별 타이핑 상태 추적을 위한 맵 (roomId:userId -> 타이핑 여부)
    private Map<String, Boolean> typingStatus = new ConcurrentHashMap<>();

    @PostConstruct
    private void init() {
        chatRooms = new LinkedHashMap<>();
    }

    // 모든 방을 찾는 메서드
    public List<ChatRoom> findAllRoom() {
        List<ChatRoomEntity> roomEntities = chatRoomRepository.findAll();

        if (roomEntities.isEmpty()) {
            return Collections.emptyList();
        }

        List<ChatRoom> rooms = new ArrayList<>();
        for (ChatRoomEntity entity : roomEntities) {
            ChatRoom chatRoom = ChatRoom.builder()
                    .roomId(entity.getRoomId())
                    .name(entity.getName())
                    .build();

            rooms.add(chatRoom);
            chatRooms.put(entity.getRoomId(), chatRoom);  // chatRooms에도 추가
        }

        return rooms;
    }


    // id로 방을 찾고 결과로 ChatRoom 객체 반환
    public ChatRoom findRoomById(String roomId) {
        ChatRoom room = chatRooms.get(roomId);

        // 메모리에 없으면 DB에서 로드 시도
        if (room == null) {
            ChatRoomEntity roomEntity = chatRoomRepository.findByRoomId(roomId).orElse(null);
            if (roomEntity != null) {
                room = ChatRoom.builder()
                        .roomId(roomEntity.getRoomId())
                        .name(roomEntity.getName())
                        .build();
                chatRooms.put(roomId, room);
            }
        }

        return room;
    }

    // 방 생성 메서드
    public ChatRoom createRoom(String name) {
        String randomId = UUID.randomUUID().toString();
        //랜덤 roomId 생성
        ChatRoom chatRoom = ChatRoom.builder() //builder로 변수 세팅
                .roomId(randomId)
                .name(name)
                .build();

        chatRooms.put(randomId, chatRoom); //방 생성 후 방 목록에 추가

        ChatRoomEntity roomEntity = ChatRoomEntity.builder()
                .roomId(randomId)
                .name(name)
                .createdAt(LocalDateTime.now())
                .build();
        chatRoomRepository.save(roomEntity);

        return chatRoom;
    }

    public <T> void sendMessage(WebSocketSession session, T message) {
        try{
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    // 메시지 읽음 상태 업데이트
    public void markMessageAsRead(String messageId, String userId) {
        ChatMessage message = messageCache.get(messageId);
        if (message != null) {
            message.markAsReadBy(userId);

            // Redis에도 업데이트
            updateMessageInRedis(message);

            // 읽음 상태 변경 알림 전송
            sendReadReceiptNotification(message, userId);
        }
    }

    // Redis에 메시지 업데이트
    private void updateMessageInRedis(ChatMessage message) {
        try {
            // 메시지 ID를 키로 사용하여 Redis에 저장
            redisTemplate.opsForValue().set("message:" + message.getMessageId(), message);

            // 읽음 상태 업데이트를 Kafka로 전송
            sendMessageToKafka("chat-read-receipts", message);
        } catch (Exception e) {
            log.error("Redis 메시지 업데이트 오류: {}", e.getMessage());
        }
    }

    // 읽음 상태 변경 알림 전송
    private void sendReadReceiptNotification(ChatMessage message, String userId) {
        ChatMessage readReceipt = new ChatMessage();
        readReceipt.setType(ChatMessage.MessageType.READ_RECEIPT);
        readReceipt.setRoomId(message.getRoomId());
        readReceipt.setSender(userId);
        readReceipt.setMessageId(message.getMessageId());

        // 해당 채팅방에 읽음 상태 알림 전송
        ChatRoom room = findRoomById(message.getRoomId());
        if (room != null) {
            room.sendMessage(readReceipt, this);
        }
    }

    // 타이핑 상태 업데이트
    public void updateTypingStatus(String roomId, String userId, boolean isTyping) {
        String key = roomId + ":" + userId;
        Boolean currentStatus = typingStatus.get(key);

        // 상태가 변경되었을 때만 처리
        if (currentStatus == null || currentStatus != isTyping) {
            typingStatus.put(key, isTyping);

            // 타이핑 상태 변경 알림 전송
            ChatMessage typingMessage = new ChatMessage();
            typingMessage.setType(isTyping ? ChatMessage.MessageType.TYPING_START : ChatMessage.MessageType.TYPING_END);
            typingMessage.setRoomId(roomId);
            typingMessage.setSender(userId);
            typingMessage.setTyping(isTyping);

            // 해당 채팅방에 타이핑 상태 알림 전송
            ChatRoom room = findRoomById(roomId);
            if (room != null) {
                room.sendMessage(typingMessage, this);
            }
        }
    }

    // 새 메시지 캐싱 메서드(기존 cacheMessage 업데이트)
    public void cacheMessage(String roomId, ChatMessage message) {
        // 메시지 ID 생성 및 설정(아직 없는 경우)
        if (message.getMessageId() == null || message.getMessageId().isEmpty()) {
            message.setMessageId(UUID.randomUUID().toString());
        }

        // 메시지 캐시에 저장
        messageCache.put(message.getMessageId(), message);

        // Redis에 저장
        redisTemplate.opsForList().rightPush("chat:" + roomId, message);
        redisTemplate.opsForValue().set("message:" + message.getMessageId(), message);
    }

    // 방에 입장할 때 이전 메시지 히스토리 조회
    public List<ChatMessage> getChatHistory(String roomId, int limit) {
        // Redis에서 최근 메시지 가져오기
        List<Object> messagesObj = redisTemplate.opsForList().range("chat:" + roomId, -limit, -1);
        List<ChatMessage> result = new ArrayList<>();

        if (messagesObj != null) {
            messagesObj.forEach(msg -> {
                if (msg instanceof ChatMessage) {
                    result.add((ChatMessage) msg);
                }
            });
        }

        return result;
    }

    public void sendMessageToKafka(String topic, ChatMessage message) {
        try {
            kafkaTemplate.send(topic, objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            log.error("Kafka 메시지 변환 오류: {}", e.getMessage());
        }
    }
}
