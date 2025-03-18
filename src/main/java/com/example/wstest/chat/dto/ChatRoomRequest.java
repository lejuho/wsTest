package com.example.wstest.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;


@Getter
public class ChatRoomRequest {
    @NotBlank(message = "Chat room name cannot be blank")
    private String name;
}
