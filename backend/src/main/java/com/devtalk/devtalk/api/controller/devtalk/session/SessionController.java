package com.devtalk.devtalk.api.controller.devtalk.session;

import com.devtalk.devtalk.domain.devtalk.session.Session;
import com.devtalk.devtalk.service.devtalk.session.SessionService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/devtalk/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService){
        this.sessionService = sessionService;
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<Session> getSession(@PathVariable("sessionId") String sessionId){
        Session session = sessionService.getOrThrow(sessionId);
        return ResponseEntity.ok(session);
    }

    @PostMapping
    public ResponseEntity<Session> createSession(){
        return ResponseEntity.ok(sessionService.create());
    }

    @PostMapping("/{sessionId}/resolve")
    public ResponseEntity<Session> resolveSession(@PathVariable("sessionId")String sessionId){
        Session session = sessionService.getOrThrow(sessionId);
        session.resolve();
        return ResponseEntity.ok(session);
    }

    @PostMapping("/{sessionId}/unresolved")
    public ResponseEntity<Session> unresolvedSession(@PathVariable("sessionId")String sessionId){
        Session session = sessionService.getOrThrow(sessionId);
        session.unresolved();
        return ResponseEntity.ok(session);
    }
}
