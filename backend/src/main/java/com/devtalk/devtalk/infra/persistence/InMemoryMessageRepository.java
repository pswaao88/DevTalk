package com.devtalk.devtalk.infra.persistence;

import com.devtalk.devtalk.domain.devtalk.message.Message;
import com.devtalk.devtalk.domain.devtalk.message.MessageRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryMessageRepository implements MessageRepository {
    private final ConcurrentMap<String, List<Message>> messagesBySession = new ConcurrentHashMap<>();

    @Override
    public Message append(String sessionId, Message message){
        List<Message> messages;
        if(messagesBySession.containsKey(sessionId)){
            messages = messagesBySession.get(sessionId);
        }else{
            messages = new ArrayList<>();
        }
        messages.add(message);
        messagesBySession.put(sessionId, messages);
        return message;
    }

    @Override
    public Optional<List<Message>> findAllBySessionId(String sessionId){
        return Optional.ofNullable(messagesBySession.get(sessionId));
    }

    @Override
    public void deleteAllBySessionId(String sessionId){
        messagesBySession.remove(sessionId);
    }
}
