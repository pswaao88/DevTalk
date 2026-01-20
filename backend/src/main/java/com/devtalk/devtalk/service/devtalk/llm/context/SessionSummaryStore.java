package com.devtalk.devtalk.service.devtalk.llm.context;

public interface SessionSummaryStore {
    String getPrefixSummary(String sessionId);
    void putPrefixSummary(String sessionId, String prefixSummary);
}
