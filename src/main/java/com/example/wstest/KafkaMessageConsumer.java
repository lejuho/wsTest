package com.example.wstest;

import com.example.wstest.dto.ChatMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@KafkaListener(topics = "chat-messages", groupId = "chat-group")
public class KafkaMessageConsumer {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChatService chatService;

    @KafkaHandler
    public void consumeMessage(String message) {

        try {
            ChatMessage chatMessage = objectMapper.readValue(message, ChatMessage.class);
            chatService.cacheMessage(chatMessage.getRoomId(), chatMessage);
        } catch (JsonProcessingException e) {
            log.error("Kafka 메시지 파싱 오류: {}", e.getMessage());
        }
    }
}

