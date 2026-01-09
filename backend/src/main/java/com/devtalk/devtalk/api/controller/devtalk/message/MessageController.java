package com.devtalk.devtalk.api.controller.devtalk.message;

import com.devtalk.devtalk.domain.devtalk.message.Message;
import com.devtalk.devtalk.service.devtalk.message.MessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/devtalk")
public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService){
        this.messageService = messageService;
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<Message> sendMessage(@PathVariable("sessionId")String sessionId, @RequestBody Message message){
        Message saved = messageService.append(sessionId, message);
        return ResponseEntity.ok(saved);
    }
}
