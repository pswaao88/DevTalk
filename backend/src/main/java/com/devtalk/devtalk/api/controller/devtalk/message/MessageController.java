package com.devtalk.devtalk.api.controller.devtalk.message;

import com.devtalk.devtalk.domain.devtalk.message.Message;
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
    public ResponseEntity<Message> sendMessage(@PathVariable("sessionId")String sessionId, @RequestBody Message message){
        Message saved = messageService.append(sessionId, message);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{sessionId}/messages")
    public ResponseEntity<List<Message>> getAllMessage(@PathVariable("sessionId")String sessionId){
        return ResponseEntity.ok(messageService.getAll(sessionId));
    }
}
