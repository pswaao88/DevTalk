package com.devtalk.devtalk.service.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.devtalk.devtalk.api.dto.response.MessageResponse;
import com.devtalk.devtalk.domain.message.Message;
import com.devtalk.devtalk.domain.message.MessageRepository;
import com.devtalk.devtalk.domain.message.MessageRole;
import com.devtalk.devtalk.domain.message.MessageStatus;
import com.devtalk.devtalk.service.llm.AiMessageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("fail")
class AiMessageServiceMockFailTest {

    @Autowired
    private AiMessageService aiMessageService;
    @Autowired
    private MessageRepository messageRepository;

    @Test
    void mock_failure_creates_failed_ai_message() {
        String sessionId = "session-fail";

        messageRepository.append(sessionId,
            new Message(MessageRole.USER, "실패 테스트", null, MessageStatus.SUCCESS)
        );

        MessageResponse ai = aiMessageService.generateAndSave(sessionId);

        assertEquals(MessageRole.AI, ai.role());
        assertEquals(MessageStatus.FAILED, ai.status());
        assertTrue(ai.content().contains("실패"));
    }
}
