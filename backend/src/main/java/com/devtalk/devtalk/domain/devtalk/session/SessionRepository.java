package com.devtalk.devtalk.domain.devtalk.session;

public interface SessionRepository {
    Session save(Session session);
    Session findById(String sessionId);
    boolean existsById(String sessionId);
}
