package com.devtalk.devtalk.api.dto.response;

import com.devtalk.devtalk.domain.message.Message;
import com.devtalk.devtalk.domain.message.MessageMarkers;
import com.devtalk.devtalk.domain.message.MessageRole;
import com.devtalk.devtalk.domain.message.MessageStatus;
import java.time.LocalDateTime;

public record MessageResponse(
    String messageId,
    MessageRole role,
    String content,
    MessageMarkers markers,
    MessageStatus status,
    LocalDateTime createdAt) {

    public static MessageResponse from(Message m){
        return new MessageResponse(
            m.getMessageId(),
            m.getRole(),
            m.getContent(),
            m.getMarkers(),
            m.getStatus(),
            m.getCreatedAt()
        );
    }
}
