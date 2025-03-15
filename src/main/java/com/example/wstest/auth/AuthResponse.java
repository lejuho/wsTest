package com.example.wstest.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthResponse {
    private String token;
    private String username;

    public AuthResponse(String token, String username) {
        this.token = token;
        this.username = username;
    }
}