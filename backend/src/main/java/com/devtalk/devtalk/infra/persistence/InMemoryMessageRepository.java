//package com.devtalk.devtalk.infra.persistence;
//
//import com.devtalk.devtalk.domain.message.Message;
//import com.devtalk.devtalk.domain.message.MessageRepository;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ConcurrentMap;
//
//@Deprecated
//public class InMemoryMessageRepository implements MessageRepository {
//    private final ConcurrentMap<String, List<Message>> messagesBySession = new ConcurrentHashMap<>();
//
//    @Override
//    public Message save(Message message){
//        messagesBySession.putIfAbsent(message.getSessionId(), new ArrayList<>());
//        List<Message> messages = messagesBySession.get(message.getSessionId());
//        messages.add(message);
//        return message;
//    }
//
//    @Override
//    public List<Message> findAllBySessionId(String sessionId){
//        List<Message> messages = messagesBySession.get(sessionId);
//        if(messages == null){
//            return List.of();
//        }
//        return List.copyOf(messages);
//    }
//
//    @Override
//    public void deleteAllBySessionId(String sessionId){
//        messagesBySession.remove(sessionId);
//    }
//}
