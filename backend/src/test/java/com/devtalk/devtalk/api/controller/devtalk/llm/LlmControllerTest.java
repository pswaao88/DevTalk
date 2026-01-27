package com.devtalk.devtalk.api.controller.devtalk.llm;

import com.devtalk.devtalk.api.dto.response.MessageResponse;
import com.devtalk.devtalk.domain.message.MessageRole;
import com.devtalk.devtalk.domain.message.MessageStatus;
import com.devtalk.devtalk.service.message.AiMessageService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LlmController.class)
public class LlmControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiMessageService aiMessageService;

    @Test
    void makeAiMessage_returns_message_response() throws Exception {
        // given
        MessageResponse response = new MessageResponse("message-1", MessageRole.USER, "테스트", null, MessageStatus.SUCCESS, LocalDateTime.now());
        given(aiMessageService.generateAndSave("session-1")).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/devtalk/sessions/session-1/ai/messages"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.messageId").value("message-1"))
            .andExpect(jsonPath("$.content").value("테스트"));
    }


}
