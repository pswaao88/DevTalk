package com.devtalk.devtalk.service.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.devtalk.devtalk.api.dto.response.MessageResponse;
import com.devtalk.devtalk.domain.message.Message;
import com.devtalk.devtalk.domain.message.MessageMetadata;
import com.devtalk.devtalk.domain.message.MessageRepository;
import com.devtalk.devtalk.domain.message.MessageRole;
import com.devtalk.devtalk.domain.message.MessageStatus;
import com.devtalk.devtalk.service.llm.AiMessageService;
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

        messageRepository.save(
            new Message(sessionId, MessageRole.USER, "테스트", null, MessageStatus.SUCCESS, MessageMetadata.empty())
        );

        MessageResponse ai = aiMessageService.generateAndSave(sessionId);

        assertEquals(MessageRole.AI, ai.role());
        assertEquals(MessageStatus.SUCCESS, ai.status());
        assertFalse(ai.content().isBlank());
    }
}
