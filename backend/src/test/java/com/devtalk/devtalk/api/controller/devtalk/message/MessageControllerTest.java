package com.devtalk.devtalk.api.controller.devtalk.message;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devtalk.devtalk.api.dto.request.SendMessageRequest;
import com.devtalk.devtalk.api.dto.response.MessageResponse;
import com.devtalk.devtalk.domain.message.MessageMetadata;
import com.devtalk.devtalk.domain.message.MessageRole;
import com.devtalk.devtalk.domain.message.MessageStatus;
import com.devtalk.devtalk.service.message.MessageService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(MessageController.class)
public class MessageControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MessageService messageService;

    @Test
    void sendMessage_returns_message_response() throws Exception {
        // given
        SendMessageRequest request =
            new SendMessageRequest("테스트", null);

        MessageResponse response =
            new MessageResponse(
                "message-1",
                MessageRole.USER,
                "테스트",
                null,
                MessageStatus.SUCCESS,
                MessageMetadata.empty(),
                LocalDateTime.now()
            );

        given(messageService.append(eq("session-1"), any(SendMessageRequest.class)))
            .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/devtalk/sessions/session-1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.messageId").value("message-1"))
            .andExpect(jsonPath("$.content").value("테스트"));
    }

    @Test
    void getAllMessage_returns_list_of_message_response() throws Exception {
        // given
        MessageResponse response =
            new MessageResponse(
                "message-1",
                MessageRole.USER,
                "테스트",
                null,
                MessageStatus.SUCCESS,
                MessageMetadata.empty(),
                LocalDateTime.now()
            );

        given(messageService.getAll("session-1"))
            .willReturn(List.of(response));

        // when & then
        mockMvc.perform(get("/api/devtalk/sessions/session-1/messages"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].messageId").value("message-1"))
            .andExpect(jsonPath("$[0].content").value("테스트"));
    }
}
