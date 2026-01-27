package com.devtalk.devtalk.domain.session;

import java.util.List;
import java.util.Optional;

public interface SessionRepository {
    Session save(Session session);
    Optional<Session> findById(String sessionId);
    boolean existsById(String sessionId);
    void deleteById(String sessionId);
    List<Session> findAll();
}
