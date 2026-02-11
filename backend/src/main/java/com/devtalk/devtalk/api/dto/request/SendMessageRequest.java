package com.devtalk.devtalk.api.dto.request;

import com.devtalk.devtalk.domain.message.Message;
import com.devtalk.devtalk.domain.message.MessageMarkers;
import com.devtalk.devtalk.domain.message.MessageMetadata;
import com.devtalk.devtalk.domain.message.MessageRole;
import com.devtalk.devtalk.domain.message.MessageStatus;

public record SendMessageRequest(
    String content,
    MessageMarkers marker
) {
    public Message toDomain(String sessionId) {
        return new Message(
            sessionId,       // 1. 여기서 받은 ID를 넣어줌
            MessageRole.USER,
            this.content(),  // 2. this.content()로 접근
            this.marker(),
            MessageStatus.SUCCESS,
            MessageMetadata.empty()
        );
    }
}
