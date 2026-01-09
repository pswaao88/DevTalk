# DESIGN-006: 개발톡(DevTalk) API 명세 (3회차 기준 · 점검 반영 확정본)

## 목적

본 문서는 Swagger 이전에 DevTalk의 API를 고정하기 위한 문서이다.

---

## 공통 원칙

- HTTP 성공/실패는 문제 해결 성공/실패를 의미하지 않는다.
- 실패 응답도 기록 자산으로 남긴다 (LLM 실패 포함).
- Resolved는 자동 판정이 아닌 **사람의 판단 종료 선언**이다.
- 상태 변경은 반드시 **SYSTEM Message**로 기록한다.

---

## Session

### 세션 생성

- POST /api/devtalk/sessions
- request:
  없음 or { "title": "" }
- response(200):
  { "sessionId": "...", "title": "새 채팅", "status": "ACTIVE", "createdAt": "..." }

---

### 세션 조회 (메시지 포함)

- GET /api/devtalk/sessions/{sessionId}
- response(200):
  { "sessionId": "...", "title": "제목", "status": "현재 상태 ACTIVE or RESOLVED", "messages": [ ... ] }

---

### 세션 목록 조회

- GET /api/devtalk/sessions
- response(200):
  [
  { "sessionId": "...", "title": "...", "status": "ACTIVE", "lastUpdatedAt": "..." },
  { "sessionId": "...", "title": "...", "status": "ACTIVE", "lastUpdatedAt": "..." }
  ]

---

### resolve (ACTIVE → RESOLVED)

- POST /api/devtalk/sessions/{sessionId}/resolve
- response(200):
  { "sessionId": "...","content": "Session resolved" ,"status": "RESOLVED" }
- 처리 시 SYSTEM 메시지 기록을 원칙으로 한다.

---

### unresolved (RESOLVED → ACTIVE)

- POST /api/devtalk/sessions/{sessionId}/unresolved
- response(200):
  { "sessionId": "...","content": "Session reopened" ,"status": "ACTIVE" }
- 처리 시 SYSTEM 메시지 기록을 원칙으로 한다.

---

## Message

### 메시지 전송 (USER)

- POST /api/devtalk/sessions/{sessionId}/messages
- request:
  { "content": "...", "markers": ["INSIGHT", "ATTEMPT"] }
- response(200):
  { "messageId": "...", "role": "USER", "status": "SUCCESS", "markers": ["INSIGHT"], "createdAt": "..." }

---

### 메시지 조회

- GET /api/devtalk/sessions/{sessionId}/messages
- response(200):
  [
  {
  "messageId": "...",
  "role": "USER",
  "content": "...",
  "status": "SUCCESS",
  "markers": ["INSIGHT"],
  "createdAt": "..."
  },
  ...
  {
  "messageId": "...",
  "role": "USER",
  "content": "...",
  "status": "SUCCESS",
  "markers": ["INSIGHT"],
  "createdAt": "..."
  }

  ]

---

## AI / LLM

### AI 응답 생성 + 기록

- POST /api/devtalk/sessions/{sessionId}/ai/messages

- 성공 response(200):
  {
  "messageId": "...",
  "role": "AI",
  "status": "SUCCESS",
  "content": "...",
  "createdAt": "..."
  }

- 실패 response(200):
  {
  "messageId": "...",
  "role": "AI",
  "status": "FAILED",
  "content": "LLM 응답 생성 실패",
  "createdAt": "..."
  }

---

## 에러 정책

- 세션 없음: 404 (메시지 기록 없음)
- 잘못된 요청 파라미터: 400
- LLM 호출 실패: 200 + FAILED 메시지 기록
