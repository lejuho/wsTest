package com.example.wstest;

import com.example.wstest.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/chat")
public class ChatController {
    private final ChatService chatService;

    @PostMapping
    public ChatRoom createRoom(@RequestBody String name) {
        return chatService.createRoom(name);
    }

    @GetMapping
    public List<ChatRoom> findAllRoom() {
        return chatService.findAllRoom();
    }

    // 이전 메시지 조회 API
    @GetMapping("/{roomId}/messages")
    public List<ChatMessage> getPreviousMessages(@PathVariable String roomId) {
        return chatService.getPreviousMessages(roomId);
    }

    // 파일 업로드 API - 채팅방별 구현
    @PostMapping("/{roomId}/upload")
    public ResponseEntity<ChatMessage> uploadFile(
            @PathVariable String roomId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(401).body(null);
            }

            ChatMessage fileMessage = chatService.uploadFile(roomId, file, userDetails.getUsername());
            return ResponseEntity.ok(fileMessage);
        } catch (IOException e) {
            log.error("파일 업로드 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    // 파일 다운로드 API
    @GetMapping("/files/{roomId}/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String roomId,
            @PathVariable String fileName,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(null);
        }

        try {
            File file = chatService.getFile(roomId, fileName);
            if (file == null) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(file.toURI());
            String contentType = determineContentType(fileName);

            // 파일 다운로드 헤더 설정
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            log.error("파일 리소스 접근 오류: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    // 파일 미리보기 API (이미지 등 브라우저에서 표시 가능한 파일)
    @GetMapping("/preview/{roomId}/{fileName:.+}")
    public ResponseEntity<Resource> previewFile(
            @PathVariable String roomId,
            @PathVariable String fileName,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(null);
        }

        try {
            File file = chatService.getFile(roomId, fileName);
            if (file == null) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(file.toURI());
            String contentType = determineContentType(fileName);

            // inline 헤더로 브라우저에서 열기
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            log.error("파일 미리보기 오류: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    private String determineContentType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "pdf":
                return "application/pdf";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls":
                return "application/vnd.ms-excel";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "mp4":
                return "video/mp4";
            case "mp3":
                return "audio/mpeg";
            default:
                return "application/octet-stream";
        }
    }
}