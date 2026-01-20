package com.devtalk.devtalk.service.devtalk.llm.context;

public interface SessionSummaryStore {
    /**
     * 세션 요약 상태 조회
     */
    SummaryState getState(String sessionId);

    /**
     * 세션 요약 상태 저장
     */
    void putState(String sessionId, SummaryState state);

    /* ===== 편의 메서드 (요약 문자열만 필요할 때) ===== */

    default String getSummary(String sessionId) {
        return getState(sessionId).summaryText();
    }

    default void putSummary(String sessionId, String summary) {
        SummaryState prev = getState(sessionId);
        putState(sessionId, new SummaryState(summary, prev.lastSummarizedIndex()));
    }
}
