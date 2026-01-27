package com.devtalk.devtalk.api.dto.response;

import com.devtalk.devtalk.domain.message.Message;
import com.devtalk.devtalk.domain.session.Session;

public record ResolveWithMessageResponse (
    ResolveResponse resolve,
    MessageResponse systemMessage
){
    public static ResolveWithMessageResponse from(Session session, Message systemMessage){
        return new ResolveWithMessageResponse(
            ResolveResponse.from(session),
            MessageResponse.from(systemMessage)
        );
    }
}
