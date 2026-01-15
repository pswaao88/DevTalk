package com.devtalk.devtalk.api.dto.request;

import com.devtalk.devtalk.domain.devtalk.message.Message;
import com.devtalk.devtalk.domain.devtalk.message.MessageMarkers;
import com.devtalk.devtalk.domain.devtalk.message.MessageRole;
import com.devtalk.devtalk.domain.devtalk.message.MessageStatus;

public record SendMessageRequest(
    String content,
    MessageMarkers marker
) {
    public static Message toDomain(SendMessageRequest sendMessageRequest){
        return new Message(MessageRole.USER, sendMessageRequest.content(), sendMessageRequest.marker(), MessageStatus.SUCCESS);
    }
}
