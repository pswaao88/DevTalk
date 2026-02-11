//package com.devtalk.devtalk.infra.persistence;
//
//import com.devtalk.devtalk.domain.llm.context.SessionSummaryStore;
//import com.devtalk.devtalk.domain.llm.context.SummaryState;
//import java.util.Map;
//import java.util.Objects;
//import java.util.concurrent.ConcurrentHashMap;
//
//@Deprecated
//public final class InMemorySessionSummaryStore implements SessionSummaryStore {
//
//    private static final String DEFAULT_SUMMARY =
//        "이전 대화 요약이 아직 없습니다. 아래 tail과 최신 질문(latestUser)을 기준으로 답변하세요.";
//
//    private final Map<String, SummaryState> store = new ConcurrentHashMap<>();
//
//    @Override
//    public SummaryState getState(String sessionId) {
//        Objects.requireNonNull(sessionId, "sessionId must not be null");
//        return store.getOrDefault(sessionId, SummaryState.empty(DEFAULT_SUMMARY));
//    }
//
//    @Override
//    public void putState(String sessionId, SummaryState state) {
//        Objects.requireNonNull(sessionId, "sessionId must not be null");
//        Objects.requireNonNull(state, "state must not be null");
//        Objects.requireNonNull(state.summaryText(), "summaryText must not be null");
//
//        if (state.lastSummarizedIndex() < 0) {
//            throw new IllegalArgumentException("lastSummarizedIndex must be >= 0");
//        }
//
//        store.put(sessionId, state);
//    }
//}
