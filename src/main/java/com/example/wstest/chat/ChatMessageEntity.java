package com.example.wstest.chat;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO) // UUID 자동 생성
    private UUID id;

    @Column(nullable = false)
    private String sender;

    @Column(nullable = false)
    private String receiver;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private boolean isRead = false;
}


