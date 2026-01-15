package com.devtalk.devtalk.domain.devtalk.session;

import java.time.LocalDateTime;
import java.util.UUID;

public class Session {
    private String sessionId;
    private String title;
    private SessionStatus status;
    private String description;
    private String aiSummary;
    private final LocalDateTime createdAt;
    private LocalDateTime lastUpdatedAt;

    // 생성시에 createdAt 설정 및 id는 UUID 사용
    public Session(){
        this.sessionId = UUID.randomUUID().toString();
        this.title = "새 채팅";
        this.status = SessionStatus.ACTIVE;
        this.description = "설명을 적어주세요.";
        this.aiSummary = "";
        this.createdAt = LocalDateTime.now();
        this.lastUpdatedAt = LocalDateTime.now();
    }
    // 해결시에 status 변경
    public void resolve(){
        this.status = SessionStatus.RESOLVED;
    }

    public void unresolved(){
        this.status = SessionStatus.ACTIVE;
    }

    public void updateLastUpdatedAt(){this.lastUpdatedAt = LocalDateTime.now(); }

    // 필드에 대한 getter
    public String getSessionId() {
        return sessionId;
    }

    public String getTitle(){return title;}

    public SessionStatus getStatus() {
        return status;
    }

    public String getDescription() { return description; }

    public String getAiSummary() { return aiSummary; }

    public LocalDateTime getLastUpdatedAt() { return lastUpdatedAt; }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
