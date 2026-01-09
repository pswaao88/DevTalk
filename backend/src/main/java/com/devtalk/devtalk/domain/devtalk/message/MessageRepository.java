package com.devtalk.devtalk.domain.devtalk.message;

import java.util.List;
import java.util.Optional;

public interface MessageRepository {
    Message append(String sessionId, Message message);
    List<Message> findAllBySessionId(String sessionId);
    void deleteAllBySessionId(String sessionId);
}
