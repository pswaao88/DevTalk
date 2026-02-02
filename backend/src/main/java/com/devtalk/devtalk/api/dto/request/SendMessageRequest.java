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
    public static Message toDomain(SendMessageRequest sendMessageRequest){
        return new Message(MessageRole.USER, sendMessageRequest.content(), sendMessageRequest.marker(), MessageStatus.SUCCESS, MessageMetadata.empty());
    }
}
