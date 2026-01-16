package com.devtalk.devtalk.api.dto.response;

import com.devtalk.devtalk.domain.devtalk.session.Session;
import com.devtalk.devtalk.domain.devtalk.session.SessionStatus;
import java.time.LocalDateTime;

public record SessionResponse(
    String sessionId,
    String title,
    SessionStatus status,
    LocalDateTime createdAt,
    LocalDateTime lastUpdatedAt
) {
    public static SessionResponse from(Session session){
        return new SessionResponse(
            session.getSessionId(),
            session.getTitle(),
            session.getStatus(),
            session.getCreatedAt(),
            session.getLastUpdatedAt()
        );
    }
}
