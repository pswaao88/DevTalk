package com.devtalk.devtalk.api.dto.response;

import com.devtalk.devtalk.domain.session.Session;
import com.devtalk.devtalk.domain.session.SessionStatus;
import java.time.LocalDateTime;

public record ResolveResponse(
    String SessionId,
    SessionStatus status,
    boolean resolved,
    LocalDateTime lastUpdatedAt
) {
    public static ResolveResponse from(Session session){
        return new ResolveResponse(
            session.getSessionId(),
            session.getStatus(),
            session.getStatus() == SessionStatus.RESOLVED,
            session.getLastUpdatedAt()
        );
    }
}
