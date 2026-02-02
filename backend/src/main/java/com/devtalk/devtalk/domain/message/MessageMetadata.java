package com.devtalk.devtalk.domain.message;

import com.devtalk.devtalk.domain.llm.LlmFinishReason;

public record MessageMetadata(
    int inputTokenCount,      // 질문(입력) 토큰 수
    int outputTokenCount,  // 답변(출력) 토큰 수
    long latencyMs,            // 응답 걸린 시간 (ms)
    LlmFinishReason finishReason // 종료 원인 (STOP, MAX_TOKENS 등)
) {
    // 메타데이터가 없는 경우(사용자 메세지 등등)
    public static MessageMetadata empty() {
        return new MessageMetadata(0, 0, 0L, LlmFinishReason.UNKNOWN);
    }
}
