package com.devtalk.devtalk.infra.persistence;

import com.devtalk.devtalk.service.devtalk.llm.context.SessionSummaryStore;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemorySessionSummaryStore implements SessionSummaryStore {

    private static final String DEFAULT_SUMMARY =
        "이전 대화 요약이 아직 없습니다. 아래 tail과 최신 질문(latestUser)을 기준으로 답변하세요.";

    private final Map<String, String> store = new ConcurrentHashMap<>();

    @Override
    public String getPrefixSummary(String sessionId) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        return store.getOrDefault(sessionId, DEFAULT_SUMMARY);
    }

    @Override
    public void putPrefixSummary(String sessionId, String prefixSummary) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(prefixSummary, "prefixSummary must not be null");
        store.put(sessionId, prefixSummary);
    }
}
