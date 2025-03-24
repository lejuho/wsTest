package com.example.wstest;

import com.example.wstest.dto.ChatMessage;
import com.example.wstest.repository.ChatMessageRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
@Service
public class ChatService {
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;
    private final Map<String, ChatRoom> chatRooms = new ConcurrentHashMap<>();
    private final Map<String, String> sessionRoomMap = new ConcurrentHashMap<>();
    private final Map<String, String> userSessionMap = new ConcurrentHashMap<>();
    private final Map<String, List<ChatMessage>> chatMessageHistory = new ConcurrentHashMap<>();

    // 파일 업로드 관련 변수
    private final String uploadDir = "uploads";
    private final long maxFileSize = 10 * 1024 * 1024; // 10MB
    private final Set<String> allowedMimeTypes = new HashSet<>();

    @PostConstruct
    private void init() {
        allowedMimeTypes.add("image/jpeg");
        allowedMimeTypes.add("image/png");
        allowedMimeTypes.add("application/pdf");
        allowedMimeTypes.add("application/msword");
        allowedMimeTypes.add("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        allowedMimeTypes.add("application/vnd.ms-excel");
        allowedMimeTypes.add("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        allowedMimeTypes.add("video/mp4");
        allowedMimeTypes.add("audio/mpeg");

        File uploadDirPath = new File(uploadDir);
        if (!uploadDirPath.exists()) {
            uploadDirPath.mkdirs();
        }
    }

    public ChatRoom createRoom(String name) {
        String roomId = UUID.randomUUID().toString();
        ChatRoom chatRoom = new ChatRoom(roomId, name);
        chatRooms.put(roomId, chatRoom);
        chatMessageHistory.put(roomId, new ArrayList<>());
        return chatRoom;
    }

    public List<ChatRoom> findAllRoom() {
        return new ArrayList<>(chatRooms.values());
    }

    public Optional<ChatRoom> findRoomById(String roomId) {
        return Optional.ofNullable(chatRooms.get(roomId));
    }

    public List<ChatMessage> getPreviousMessages(String roomId) {
        return chatMessageHistory.getOrDefault(roomId, Collections.emptyList());
    }

    public ChatMessage uploadFile(String roomId, MultipartFile file, String username) throws IOException {
        // 채팅방 존재 확인
        if (!chatRooms.containsKey(roomId)) {
            throw new IllegalArgumentException("채팅방이 존재하지 않습니다.");
        }

        // 파일 타입 확인
        if (file.getContentType() != null && !allowedMimeTypes.contains(file.getContentType())) {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다.");
        }

        // 파일 크기 확인
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("파일 크기가 제한을 초과했습니다.");
        }

        // 채팅방별 디렉토리 생성
        File roomDir = new File(uploadDir + "/" + roomId);
        if (!roomDir.exists()) {
            roomDir.mkdirs();
        }

        // 고유한 파일 이름 생성
        String originalFilename = file.getOriginalFilename();
        String savedFileName = UUID.randomUUID() + "-" + (originalFilename != null ? originalFilename : "unnamed");
        String filePath = roomDir.getPath() + "/" + savedFileName;

        // 파일 저장
        file.transferTo(new File(filePath));

        // 파일 메시지 생성 및 저장
        ChatMessage fileMessage = new ChatMessage();
        fileMessage.setRoomId(roomId);
        fileMessage.setSender(username);
        fileMessage.setType(ChatMessage.MessageType.FILE);
        fileMessage.setMessage(originalFilename);
        fileMessage.setSavedFileName(savedFileName);
        fileMessage.setTimestamp(new Date());

        // 메시지 저장 및 전송
        chatMessageHistory.get(roomId).add(fileMessage);
        chatMessageRepository.save(fileMessage);
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, fileMessage);

        return fileMessage;
    }

    public File getFile(String roomId, String fileName) {
        File file = new File(uploadDir + "/" + roomId + "/" + fileName);
        if (file.exists() && file.isFile()) {
            return file;
        }
        return null;
    }

    public void sendMessage(ChatMessage chatMessage) {
        chatMessageHistory.get(chatMessage.getRoomId()).add(chatMessage);
        chatMessageRepository.save(chatMessage);
        messagingTemplate.convertAndSend("/topic/chat/" + chatMessage.getRoomId(), chatMessage);
    }
}