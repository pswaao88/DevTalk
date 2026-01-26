package com.devtalk.devtalk.api.controller.devtalk.llm;

import com.devtalk.devtalk.service.devtalk.message.AiStreamService;
import java.time.Duration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping(("/api/devtalk/sessions"))
public class LlmStreamController {
    private final AiStreamService aiStreamService;

    public LlmStreamController (AiStreamService aiStreamService){
        this.aiStreamService = aiStreamService;
    }

    @GetMapping(value = "/{sessionId}/ai/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable String sessionId, @RequestParam(required = false) String replyTo) {
        // 3분 타임아웃(프론트가 끊기면 자동 종료됨)
        SseEmitter emitter = new SseEmitter(Duration.ofMinutes(3).toMillis());
        aiStreamService.streamAi(sessionId, replyTo, emitter);
        return emitter;
    }
}
