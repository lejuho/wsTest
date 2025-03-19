package com.example.wstest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {
    public enum MessageType {
        ENTER, TALK, LEAVE, FILE
    }

    private MessageType type;
    private String roomId;
    private String sender;
    private String message;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private Date timestamp;

    // 파일 관련 필드
    private String fileName;
    private String fileUrl;
    private long fileSize;
    private String fileMimeType;

    // 파일 크기 포맷팅
    public String getFormattedFileSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.2f KB", fileSize / 1024.0);
        } else if (fileSize < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", fileSize / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", fileSize / (1024.0 * 1024 * 1024));
        }
    }

    // 파일 타입 판별
    public boolean isImage() {
        return fileMimeType != null && fileMimeType.startsWith("image/");
    }

    public boolean isVideo() {
        return fileMimeType != null && fileMimeType.startsWith("video/");
    }

    public boolean isAudio() {
        return fileMimeType != null && fileMimeType.startsWith("audio/");
    }

    public boolean isPdf() {
        return fileMimeType != null && fileMimeType.equals("application/pdf");
    }
}