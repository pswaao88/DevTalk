package com.devtalk.devtalk.config;

import com.devtalk.devtalk.domain.message.MessageRepository;
import com.devtalk.devtalk.domain.session.SessionRepository;
import com.devtalk.devtalk.infra.persistence.InMemoryMessageRepository;
import com.devtalk.devtalk.infra.persistence.InMemorySessionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PersistenceConfig {

//    @Bean
//    public SessionRepository sessionRepository() {
//        return new InMemorySessionRepository();
//    }
//
//    @Bean
//    public MessageRepository messageRepository() {
//        return new InMemoryMessageRepository();
//    }
}
