package com.devtalk.devtalk.service.devtalk.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.devtalk.devtalk.api.dto.response.MessageResponse;
import com.devtalk.devtalk.domain.message.Message;
import com.devtalk.devtalk.domain.message.MessageRepository;
import com.devtalk.devtalk.domain.message.MessageRole;
import com.devtalk.devtalk.domain.message.MessageStatus;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("geminiSmoke")
class AiMessageServiceGeminiSmokeTest {

    @Autowired
    private AiMessageService aiMessageService;
    @Autowired
    private MessageRepository messageRepository;

    @Test
    void gemini_smoke_test() {
        Assumptions.assumeTrue(
            System.getenv("LLM_GEMINI_API_KEY") != null
        );

        String sessionId = "session-gemini";

        messageRepository.append(sessionId,
            new Message(MessageRole.USER, "한 문장으로 답해줘", null, MessageStatus.SUCCESS)
        );

        MessageResponse ai = aiMessageService.generateAndSave(sessionId);

        assertEquals(MessageRole.AI, ai.role());
        assertFalse(ai.content().isBlank());
    }
}

