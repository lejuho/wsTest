package com.example.wstest.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;



@Getter
public class TypingStatusRequest {
    @NotBlank(message = "User ID cannot be blank")
    private String userId;

    @NotNull(message = "isTyping cannot be null")
    private Boolean isTyping;
}
