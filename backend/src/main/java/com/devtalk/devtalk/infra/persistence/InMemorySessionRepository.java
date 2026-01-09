package com.devtalk.devtalk.infra.persistence;

import com.devtalk.devtalk.domain.devtalk.session.Session;
import com.devtalk.devtalk.domain.devtalk.session.SessionRepository;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemorySessionRepository implements SessionRepository {

    private final ConcurrentMap<String, Session> sessions = new ConcurrentHashMap<>();

    @Override
    public Session save(Session session){
        String sessionId = session.getSessionId();
        sessions.put(sessionId, session);
        return session;
    }
    @Override
    public Optional<Session> findById(String sessionId){
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public boolean existsById(String sessionId){
        return sessions.containsKey(sessionId);
    }

    @Override
    public void deleteById(String sessionId){
        sessions.remove(sessionId);
    }

}
