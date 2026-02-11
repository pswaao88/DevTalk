package com.devtalk.devtalk.service.session;

import com.devtalk.devtalk.api.dto.request.CreateSessionRequest;
import com.devtalk.devtalk.api.dto.request.UpdateSessionRequest;
import com.devtalk.devtalk.api.dto.response.ResolveWithMessageResponse;
import com.devtalk.devtalk.api.dto.response.SessionResponse;
import com.devtalk.devtalk.api.dto.response.SessionSummaryResponse;
import com.devtalk.devtalk.api.dto.response.SessionUpdateResponse;
import com.devtalk.devtalk.domain.llm.LlmTokenUsage;
import com.devtalk.devtalk.domain.message.Message;
import com.devtalk.devtalk.domain.message.MessageMetadata;
import com.devtalk.devtalk.domain.message.MessageRepository;
import com.devtalk.devtalk.domain.message.MessageRole;
import com.devtalk.devtalk.domain.message.MessageStatus;
import com.devtalk.devtalk.domain.session.Session;
import com.devtalk.devtalk.domain.session.SessionRepository;
import java.util.List;
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

    public SessionUpdateResponse update(String sessionId, UpdateSessionRequest updateSessionRequest){
        Session session = getSession(sessionId);
        if(updateSessionRequest.title() != null){
            session.updateTitle(updateSessionRequest.title());
        }
        if(updateSessionRequest.description() != null){
            session.updateDescription(updateSessionRequest.description());
        }
        return SessionUpdateResponse.from(sessionRepository.save(session));
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

    public ResolveWithMessageResponse resolve(String sessionId){
        Session session = getSession(sessionId);
        session.resolve();
        sessionRepository.save(session);
        Message systemMessage = new Message(sessionId, MessageRole.SYSTEM, "해당 세션이 Resolved로 변경되었습니다.", null, MessageStatus.SUCCESS,  MessageMetadata.empty());
        session.updateLastUpdatedAt();
        Message savedMessage = messageRepository.save(systemMessage);

        return ResolveWithMessageResponse.from(session, savedMessage);
    }

    public ResolveWithMessageResponse unresolve(String sessionId){
        Session session = getSession(sessionId);
        session.unresolved();
        sessionRepository.save(session);
        Message systemMessage = new Message(sessionId, MessageRole.SYSTEM, "해당 세션이 Active로 변경되었습니다.", null, MessageStatus.SUCCESS, MessageMetadata.empty());
        session.updateLastUpdatedAt();

        Message savedMessage = messageRepository.save(systemMessage);
        return ResolveWithMessageResponse.from(session, savedMessage);
    }
}
