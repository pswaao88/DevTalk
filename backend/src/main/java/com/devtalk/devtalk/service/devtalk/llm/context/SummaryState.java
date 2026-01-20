package com.devtalk.devtalk.service.devtalk.llm.context;

public record SummaryState(
    String summaryText,
    int lastSummarizedIndex
) {
    public static SummaryState empty(String defaultSummary) {
        return new SummaryState(defaultSummary, 0);
    }
}
