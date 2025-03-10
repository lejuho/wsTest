package com.example.wstest;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController //HTTP 바디에 Json으로 써야하기 때문에 RestController 사용
@RequestMapping("/chat") //URL 매핑
public class ChatController {
    private final ChatService chatService;

    @PostMapping
    public ChatRoom createRoom(@RequestBody String name) {
        return chatService.createRoom(name);
        //Post 요청이 들어올 시, Json에서 name 값을 받아 방을 생성한다.
    }

    @GetMapping
    public List<ChatRoom> findAllRoom() {
        return chatService.findAllRoom();
        //Get 요청이 들어올 시, 모든 방 목록을 조회한다.
    }
}
