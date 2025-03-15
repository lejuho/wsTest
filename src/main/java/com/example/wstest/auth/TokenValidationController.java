package com.example.wstest.auth;

import com.example.wstest.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class TokenValidationController {

    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestParam String token) {
        boolean isValid = jwtTokenProvider.validateToken(token);

        if (isValid) {
            String username = jwtTokenProvider.getUsername(token);
            return ResponseEntity.ok().body("Token is valid for user: " + username);
        } else {
            return ResponseEntity.badRequest().body("Invalid token");
        }
    }
}
