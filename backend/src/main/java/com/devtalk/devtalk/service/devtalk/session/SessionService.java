package com.devtalk.devtalk.service.devtalk.session;

import com.devtalk.devtalk.api.dto.request.CreateSessionRequest;
import com.devtalk.devtalk.api.dto.response.SessionResponse;
import com.devtalk.devtalk.api.dto.response.SessionSummaryResponse;
import com.devtalk.devtalk.domain.devtalk.message.Message;
import com.devtalk.devtalk.domain.devtalk.message.MessageRepository;
import com.devtalk.devtalk.domain.devtalk.message.MessageRole;
import com.devtalk.devtalk.domain.devtalk.message.MessageStatus;
import com.devtalk.devtalk.domain.devtalk.session.Session;
import com.devtalk.devtalk.domain.devtalk.session.SessionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SessionService {
    private final SessionRepository sessionRepository;
    private final MessageRepository messageRepository;

    public SessionService(SessionRepository sessionRepository, MessageRepository messageRepository){
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
    }

    public SessionResponse create(CreateSessionRequest createSessionRequest){
        Session session = new Session(createSessionRequest.title());
        return SessionResponse.from(sessionRepository.save(session));
    }

    private Session getSession(String sessionId){
        Session session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("session not found"));
        return session;
    }

    public SessionResponse getOrThrow(String sessionId){
        return SessionResponse.from(getSession(sessionId));
    }

    public List<SessionSummaryResponse> getAllSession(){
        return sessionRepository.findAll().stream().map(SessionSummaryResponse::from).toList();
    }

    public boolean exist(String sessionId){
        return sessionRepository.existsById(sessionId);
    }

    public void delete(String sessionId){
        sessionRepository.deleteById(sessionId);
    }

    public Message resolve(String sessionId){
        Session session = getSession(sessionId);
        session.resolve();
        Message systemMessage = new Message(MessageRole.SYSTEM, "해당 세션이 Resolved로 변경되었습니다.", null, MessageStatus.SUCCESS);
        session.updateLastUpdatedAt();
        return messageRepository.append(sessionId, systemMessage);
    }

    public Message unresolve(String sessionId){
        Session session = getSession(sessionId);
        session.unresolved();
        Message systemMessage = new Message(MessageRole.SYSTEM, "해당 세션이 Active로 변경되었습니다.", null, MessageStatus.SUCCESS);
        session.updateLastUpdatedAt();
        return messageRepository.append(sessionId, systemMessage);
    }
}
