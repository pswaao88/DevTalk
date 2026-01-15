package com.devtalk.devtalk.service.devtalk.message;

import com.devtalk.devtalk.domain.devtalk.message.Message;
import com.devtalk.devtalk.domain.devtalk.message.MessageRepository;
import com.devtalk.devtalk.domain.devtalk.session.Session;
import com.devtalk.devtalk.domain.devtalk.session.SessionRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MessageService {
    private final MessageRepository messageRepository;
    private final SessionRepository sessionRepository;

    public MessageService(MessageRepository messageRepository, SessionRepository sessionRepository){
        this.messageRepository = messageRepository;
        this.sessionRepository = sessionRepository;
    }

    public Message append(String sessionId, Message message){
        verifySession(sessionId);
        Session session = sessionRepository.findById(sessionId).get();
        session.updateLastUpdatedAt();
        return messageRepository.append(sessionId, message);
    }

    public List<Message> getAll(String sessionId){
        verifySession(sessionId);
        return messageRepository.findAllBySessionId(sessionId);
    }

    public void deleteAll(String sessionId){
        verifySession(sessionId);
        messageRepository.deleteAllBySessionId(sessionId);
    }

    private Session verifySession(String sessionId){
        return sessionRepository.findById(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("session not found"));
    }
}
