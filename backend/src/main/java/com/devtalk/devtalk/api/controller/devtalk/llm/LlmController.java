package com.devtalk.devtalk.api.controller.devtalk.llm;

import com.devtalk.devtalk.domain.devtalk.message.Message;
import com.devtalk.devtalk.service.devtalk.message.AiMessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/devtalk/sessions")
public class LlmController {
    private final AiMessageService aiMessageService;

    public LlmController(AiMessageService aiMessageService){
        this.aiMessageService = aiMessageService;
    }
    // 이미 질문에 대한 메세지는 가장 최신 메세지로 들어갔기 때문에 따로 받을 필요 X
    @PostMapping("{sessionId}/ai/messages")
    public ResponseEntity<Message> makeAiMessage(@PathVariable("sessionId")String sessionId){
        return ResponseEntity.ok(aiMessageService.generateAndSave(sessionId));
    }
}
