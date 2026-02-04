package com.devtalk.devtalk.domain.message;

import java.util.List;

public interface MessageRepository {
    Message save(Message message);
    List<Message> findAllBySessionId(String sessionId);
    void deleteAllBySessionId(String sessionId);
}
