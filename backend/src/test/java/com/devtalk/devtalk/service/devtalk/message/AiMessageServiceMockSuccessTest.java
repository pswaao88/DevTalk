package com.devtalk.devtalk.service.devtalk.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.devtalk.devtalk.domain.devtalk.message.Message;
import com.devtalk.devtalk.domain.devtalk.message.MessageRepository;
import com.devtalk.devtalk.domain.devtalk.message.MessageRole;
import com.devtalk.devtalk.domain.devtalk.message.MessageStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AiMessageServiceMockSuccessTest {

    @Autowired
    private AiMessageService aiMessageService;
    @Autowired
    private MessageRepository messageRepository;

    @Test
    void mock_success_creates_ai_message() {
        String sessionId = "session-success";

        messageRepository.append(sessionId,
            new Message("1", MessageRole.USER, "테스트", null, MessageStatus.SUCCESS)
        );

        Message ai = aiMessageService.generateAndSave(sessionId);

        assertEquals(MessageRole.AI, ai.getRole());
        assertEquals(MessageStatus.SUCCESS, ai.getStatus());
        assertFalse(ai.getContent().isBlank());
    }
}
