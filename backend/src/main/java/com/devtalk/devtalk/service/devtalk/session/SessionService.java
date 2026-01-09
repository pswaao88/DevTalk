package com.devtalk.devtalk.service.devtalk.session;

import com.devtalk.devtalk.domain.devtalk.session.Session;
import com.devtalk.devtalk.domain.devtalk.session.SessionRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SessionService {
    private final SessionRepository sessionRepository;

    public SessionService(SessionRepository sessionRepository){
        this.sessionRepository = sessionRepository;
    }

    public Session create(){
        Session session = new Session();
        return sessionRepository.save(session);
    }

    public Session getOrThrow(String sessionId){
        Session session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("session not found"));
        return session;
    }

    public List<Session> getAllSession(){
        return sessionRepository.findAll();
    }

    public boolean exist(String sessionId){
        return sessionRepository.existsById(sessionId);
    }

    public void delete(String sessionId){
        sessionRepository.deleteById(sessionId);
    }
}
