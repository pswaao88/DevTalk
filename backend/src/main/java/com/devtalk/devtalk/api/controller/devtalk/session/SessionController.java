package com.devtalk.devtalk.api.controller.devtalk.session;

import com.devtalk.devtalk.api.dto.request.CreateSessionRequest;
import com.devtalk.devtalk.api.dto.response.ResolveWithMessageResponse;
import com.devtalk.devtalk.api.dto.response.SessionResponse;
import com.devtalk.devtalk.api.dto.response.SessionSummaryResponse;
import com.devtalk.devtalk.service.session.SessionService;
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
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService){
        this.sessionService = sessionService;
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<SessionResponse> getSession(@PathVariable("sessionId") String sessionId){
        return ResponseEntity.ok(sessionService.getOrThrow(sessionId));
    }

    @PostMapping
    public ResponseEntity<SessionResponse> createSession(@RequestBody CreateSessionRequest createSessionRequest){
        return ResponseEntity.ok(sessionService.create(createSessionRequest));
    }

    @GetMapping()
    public ResponseEntity<List<SessionSummaryResponse>> getSessionList(){
        return ResponseEntity.ok(sessionService.getAllSession());
    }

    @PostMapping("/{sessionId}/resolve")
    public ResponseEntity<ResolveWithMessageResponse> resolveSession(@PathVariable("sessionId")String sessionId){
        return ResponseEntity.ok(sessionService.resolve(sessionId));
    }

    @PostMapping("/{sessionId}/unresolved")
    public ResponseEntity<ResolveWithMessageResponse> unresolvedSession(@PathVariable("sessionId")String sessionId){
        return ResponseEntity.ok(sessionService.unresolve(sessionId));
    }
}
