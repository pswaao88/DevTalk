package com.devtalk.devtalk.api.controller.devtalk.session;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devtalk.devtalk.api.dto.request.CreateSessionRequest;
import com.devtalk.devtalk.api.dto.response.ResolveResponse;
import com.devtalk.devtalk.api.dto.response.ResolveWithMessageResponse;
import com.devtalk.devtalk.api.dto.response.SessionResponse;
import com.devtalk.devtalk.api.dto.response.SessionSummaryResponse;
import com.devtalk.devtalk.api.dto.response.MessageResponse;
import com.devtalk.devtalk.domain.message.MessageMetadata;
import com.devtalk.devtalk.domain.message.MessageRole;
import com.devtalk.devtalk.domain.message.MessageStatus;
import com.devtalk.devtalk.domain.session.SessionStatus;


import com.devtalk.devtalk.service.session.SessionService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(SessionController.class)
public class SessionControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SessionService sessionService;

    @Test
    void getSession_return_SessionResponse() throws Exception {
        // given
        SessionResponse response =
            new SessionResponse(
                "session-1",
                "새 채팅",
                SessionStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now()
            );

        given(sessionService.getOrThrow("session-1"))
            .willReturn(response);

        // when & then
        mockMvc.perform(get("/api/devtalk/sessions/session-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").value("session-1"))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createSession_return_SessionResponse() throws Exception {
        // given
        CreateSessionRequest request =
            new CreateSessionRequest("새 채팅");

        SessionResponse response =
            new SessionResponse(
                "session-1",
                "새 채팅",
                SessionStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now()
            );

        given(sessionService.create(any(CreateSessionRequest.class)))
            .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/devtalk/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").value("session-1"))
            .andExpect(jsonPath("$.title").value("새 채팅"));
    }

    @Test
    void getSessionList_return_SessionSummaryResponseList() throws Exception {
        // given
        SessionSummaryResponse summary =
            new SessionSummaryResponse(
                "session-1",
                "새 채팅",
                SessionStatus.ACTIVE,
                "설명",
                "ai설명",
                LocalDateTime.now(),
                LocalDateTime.now()
            );

        given(sessionService.getAllSession())
            .willReturn(List.of(summary));

        // when & then
        mockMvc.perform(get("/api/devtalk/sessions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].sessionId").value("session-1"))
            .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void resolveSession_return_ResolveWithMessageResponse() throws Exception {
        // given
        ResolveResponse resolve =
            new ResolveResponse(
                "session-1",
                SessionStatus.RESOLVED,
                true,
                LocalDateTime.now()
            );

        MessageResponse systemMessage =
            new MessageResponse(
                "msg-1",
                MessageRole.SYSTEM,
                "Resolved로 변경",
                null,
                MessageStatus.SUCCESS,
                MessageMetadata.empty(),
                LocalDateTime.now()
            );

        ResolveWithMessageResponse response =
            new ResolveWithMessageResponse(resolve, systemMessage);

        given(sessionService.resolve("session-1"))
            .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/devtalk/sessions/session-1/resolve"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resolve.resolved").value(true))
            .andExpect(jsonPath("$.systemMessage.role").value("SYSTEM"));
    }

    @Test
    void unresolveSession_return_ResolveWithMessageResponse() throws Exception {
        // given
        ResolveResponse resolve =
            new ResolveResponse(
                "session-1",
                SessionStatus.ACTIVE,
                false,
                LocalDateTime.now()
            );

        MessageResponse systemMessage =
            new MessageResponse(
                "msg-2",
                MessageRole.SYSTEM,
                "Unresolved로 변경",
                null,
                MessageStatus.SUCCESS,
                MessageMetadata.empty(),
                LocalDateTime.now()
            );

        ResolveWithMessageResponse response =
            new ResolveWithMessageResponse(resolve, systemMessage);

        given(sessionService.unresolve("session-1"))
            .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/devtalk/sessions/session-1/unresolved"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resolve.resolved").value(false))
            .andExpect(jsonPath("$.systemMessage.role").value("SYSTEM"));
    }
}
