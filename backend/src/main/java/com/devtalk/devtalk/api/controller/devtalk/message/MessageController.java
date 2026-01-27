package com.devtalk.devtalk.api.controller.devtalk.message;

import com.devtalk.devtalk.api.dto.request.SendMessageRequest;
import com.devtalk.devtalk.api.dto.response.MessageResponse;
import com.devtalk.devtalk.service.devtalk.message.MessageService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/devtalk/sessions")
public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService){
        this.messageService = messageService;
    }

    @PostMapping("/{sessionId}/messages")
    public ResponseEntity<MessageResponse> sendMessage(@PathVariable("sessionId")String sessionId, @RequestBody SendMessageRequest sendMessageRequest){
        return ResponseEntity.ok(messageService.append(sessionId, sendMessageRequest));
    }

    @GetMapping("/{sessionId}/messages")
    public ResponseEntity<List<MessageResponse>> getAllMessage(@PathVariable("sessionId")String sessionId){
        return ResponseEntity.ok(messageService.getAll(sessionId));
    }
}
