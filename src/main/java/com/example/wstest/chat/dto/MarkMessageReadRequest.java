package com.example.wstest.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;


@Getter
public class MarkMessageReadRequest {
    @NotBlank(message = "User ID cannot be blank")
    private String userId;
}
