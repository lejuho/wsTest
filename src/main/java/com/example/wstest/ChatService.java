package com.example.wstest;

import com.example.wstest.dto.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@RequiredArgsConstructor
@Service
public class ChatService {

    private final ObjectMapper objectMapper;
    private Map<String, ChatRoom> chatRooms = new ConcurrentHashMap<>();
    private Map<String, List<ChatMessage>> chatMessageHistory = new ConcurrentHashMap<>();

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${file.max-size:10485760}") // 기본 10MB
    private long maxFileSize;

    @Value("${file.allowed-types:image/jpeg,image/png,image/gif,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document}")
    private String allowedTypes;

    private Set<String> allowedMimeTypes;

    @PostConstruct
    public void init() {
        // 허용된 MIME 타입 초기화
        allowedMimeTypes = new HashSet<>(Arrays.asList(allowedTypes.split(",")));

        // 업로드 디렉토리 생성
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        log.info("파일 업로드 서비스 초기화: 디렉토리={}, 최대크기={}바이트, 허용타입={}",
                uploadDir, maxFileSize, allowedMimeTypes);
    }

    public List<ChatRoom> findAllRoom() {
        return new ArrayList<>(chatRooms.values());
    }

    public ChatRoom findRoomById(String roomId) {
        return chatRooms.get(roomId);
    }

    public ChatRoom createRoom(String name) {
        String randomId = UUID.randomUUID().toString();
        ChatRoom chatRoom = ChatRoom.builder()
                .roomId(randomId)
                .name(name)
                .build();
        chatRooms.put(randomId, chatRoom);

        // 채팅방 생성 시 메시지 히스토리 초기화
        chatMessageHistory.put(randomId, new CopyOnWriteArrayList<>());

        return chatRoom;
    }

    public <T> void sendMessage(WebSocketSession session, T message) throws IOException {
        String payload = objectMapper.writeValueAsString(message);
        session.sendMessage(new TextMessage(payload));

        // 메시지 저장 로직
        if (message instanceof ChatMessage) {
            saveChatMessage((ChatMessage) message);
        }
    }

    private void saveChatMessage(ChatMessage chatMessage) {
        String roomId = chatMessage.getRoomId();
        if (!chatMessageHistory.containsKey(roomId)) {
            chatMessageHistory.put(roomId, new CopyOnWriteArrayList<>());
        }
        chatMessageHistory.get(roomId).add(chatMessage);

        // 메시지 개수 제한 (선택적으로 구현)
        if (chatMessageHistory.get(roomId).size() > 200) {
            chatMessageHistory.get(roomId).remove(0);
        }
    }

    public List<ChatMessage> getPreviousMessages(String roomId) {
        return chatMessageHistory.getOrDefault(roomId, Collections.emptyList());
    }

    public ChatMessage uploadFile(String roomId, MultipartFile file, String username) throws IOException {
        // 1. 파일 유효성 검사
        validateFile(file);

        // 2. 파일 저장
        FileInfo fileInfo = saveFile(file, roomId);

        // 3. 파일 메시지 생성
        ChatMessage fileMessage = new ChatMessage();
        fileMessage.setType(ChatMessage.MessageType.FILE);
        fileMessage.setRoomId(roomId);
        fileMessage.setSender(username);
        fileMessage.setFileName(fileInfo.getOriginalFileName());
        fileMessage.setFileUrl(fileInfo.getFileUrl());
        fileMessage.setFileSize(fileInfo.getFileSize());
        fileMessage.setFileMimeType(fileInfo.getMimeType());
        fileMessage.setTimestamp(new Date());

        // 4. 메시지 텍스트 설정
        String fileType = getFileTypeLabel(fileInfo.getMimeType());
        fileMessage.setMessage(username + "님이 " + fileType + " 파일을 공유했습니다: " + fileInfo.getOriginalFileName());

        // 5. 방에 메시지 전송
        ChatRoom chatRoom = findRoomById(roomId);
        if (chatRoom != null) {
            chatRoom.sendMessageToAll(fileMessage, this);
            saveChatMessage(fileMessage);
        }

        return fileMessage;
    }

    private void validateFile(MultipartFile file) throws IOException {
        // 파일 크기 검사
        if (file.getSize() > maxFileSize) {
            throw new IOException("파일 크기가 제한을 초과합니다. 최대 " + (maxFileSize / 1048576) + "MB까지 허용됩니다.");
        }

        // MIME 타입 검사
        String contentType = file.getContentType();
        if (contentType == null || !allowedMimeTypes.contains(contentType)) {
            throw new IOException("허용되지 않는 파일 형식입니다. 허용된 형식: " + allowedMimeTypes);
        }
    }

    private FileInfo saveFile(MultipartFile file, String roomId) throws IOException {
        // 채팅방별 디렉토리 생성
        String roomDirectory = uploadDir + File.separator + roomId;
        File dir = new File(roomDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 고유한 파일명 생성
        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

        // 파일 저장
        File dest = new File(dir, uniqueFileName);
        file.transferTo(dest);

        // 파일 정보 반환
        FileInfo fileInfo = new FileInfo();
        fileInfo.setOriginalFileName(originalFileName);
        fileInfo.setSavedFileName(uniqueFileName);
        fileInfo.setFileUrl("/api/files/" + roomId + "/" + uniqueFileName);
        fileInfo.setFileSize(file.getSize());
        fileInfo.setMimeType(file.getContentType());

        return fileInfo;
    }

    public File getFile(String roomId, String fileName) {
        String filePath = uploadDir + File.separator + roomId + File.separator + fileName;
        File file = new File(filePath);

        if (!file.exists() || !file.isFile()) {
            return null;
        }

        return file;
    }

    private String getFileTypeLabel(String mimeType) {
        if (mimeType == null) return "알 수 없는 형식의";

        if (mimeType.startsWith("image/")) {
            return "이미지";
        } else if (mimeType.startsWith("video/")) {
            return "동영상";
        } else if (mimeType.startsWith("audio/")) {
            return "오디오";
        } else if (mimeType.equals("application/pdf")) {
            return "PDF";
        } else if (mimeType.contains("word") || mimeType.contains("document")) {
            return "문서";
        } else if (mimeType.contains("sheet") || mimeType.contains("excel")) {
            return "스프레드시트";
        } else if (mimeType.contains("presentation") || mimeType.contains("powerpoint")) {
            return "프레젠테이션";
        } else {
            return "첨부";
        }
    }

    // 파일 정보를 담기 위한 내부 클래스
    @lombok.Data
    private static class FileInfo {
        private String originalFileName;
        private String savedFileName;
        private String fileUrl;
        private long fileSize;
        private String mimeType;
    }
}