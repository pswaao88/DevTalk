package com.devtalk.devtalk.api.dto.response;

import com.devtalk.devtalk.domain.session.Session;
import com.devtalk.devtalk.domain.session.SessionStatus;
import java.time.LocalDateTime;

public record SessionUpdateResponse(
    String sessionId,
    String title,
    SessionStatus status,
    String description,
    LocalDateTime createdAt,
    LocalDateTime lastUpdatedAt
) {
    public static SessionUpdateResponse from(Session session){
        return new SessionUpdateResponse(
            session.getSessionId(),
            session.getTitle(),
            session.getStatus(),
            session.getDescription(),
            session.getCreatedAt(),
            session.getLastUpdatedAt()
        );
    }
}
