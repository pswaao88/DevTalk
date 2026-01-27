package com.devtalk.devtalk.domain.message;

import java.time.LocalDateTime;
import java.util.UUID;

public class Message {
    private final String messageId;
    private final MessageRole role;
    private final String content;
    private MessageMarkers markers;
    private final MessageStatus status;
    private final LocalDateTime createdAt;

    public Message(MessageRole role, String content, MessageMarkers markers ,MessageStatus status) {
        this.messageId = UUID.randomUUID().toString();
        this.role = role;
        this.content = content;
        this.markers = markers;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    public String getMessageId() {
        return messageId;
    }

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

}
