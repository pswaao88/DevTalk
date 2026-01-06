# LOG-001-Resolved상태변경기록예시

## 발생 시점
2026-01-06 ~

## 세션 흐름 예시

[USER] 특정 API 호출 시 NullPointerException 발생  
[AI] 원인 분석 및 수정 제안  
[SYSTEM] Session 상태 변경: RESOLVED (2026-01-06 17:42)

[USER] 며칠 뒤 동일 증상 재발  
[AI] 추가 원인 분석  
[SYSTEM] Session 상태 변경: ACTIVE (Resolved 판단 철회, 2026-01-08 10:10)

[USER] 다른 접근 방식으로 수정 시도  
[AI] 수정 방향 검증

## 비고
- Session 상태는 현재 판단 상태를 나타낸다.
- 상태 변경 이력은 판단 흐름을 이해하기 위한 기록이다.
- 메시지 흐름은 Session 상태와 무관하게 이어진다.
