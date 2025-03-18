package com.example.wstest.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, UUID> {
}
