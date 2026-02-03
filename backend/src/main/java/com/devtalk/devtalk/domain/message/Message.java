package com.devtalk.devtalk.domain.message;

import java.time.LocalDateTime;
import java.util.UUID;

public class Message {
    private final String messageId;
    private final String sessionId; // FK
    private final MessageRole role;
    private final String content;
    private MessageMarkers markers;
    private final MessageStatus status;
    private final MessageMetadata messageMetadata;
    private final LocalDateTime createdAt;

    public Message(String sessionId, MessageRole role, String content, MessageMarkers markers , MessageStatus status, MessageMetadata messageMetadata) {
        this.messageId = UUID.randomUUID().toString();
        this.sessionId = sessionId;
        this.role = role;
        this.content = content;
        this.markers = markers;
        this.status = status;
        this.messageMetadata = messageMetadata;
        this.createdAt = LocalDateTime.now();
    }
    // DB용 생성자
    public Message(String messageId, String sessionId, MessageRole role, String content, MessageMarkers markers,
        MessageStatus status, MessageMetadata messageMetadata, LocalDateTime createdAt) {
        this.messageId = messageId;
        this.sessionId = sessionId;
        this.role = role;
        this.content = content;
        this.markers = markers;
        this.status = status;
        this.messageMetadata = messageMetadata;
        this.createdAt = createdAt;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getSessionId() { return sessionId; }

    public MessageRole getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public MessageMarkers getMarkers(){return markers;}

    public MessageStatus getStatus() {
        return status;
    }

    public MessageMetadata getMessageMetadata(){return messageMetadata;}

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

}
