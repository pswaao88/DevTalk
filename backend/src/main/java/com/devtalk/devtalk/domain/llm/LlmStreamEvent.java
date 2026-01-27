package com.devtalk.devtalk.domain.llm;

public record LlmStreamEvent(String delta, LlmFinishReason finishReason) {
    public static LlmStreamEvent delta(String d) { return new LlmStreamEvent(d, LlmFinishReason.UNKNOWN); }
    public static LlmStreamEvent done(LlmFinishReason r) { return new LlmStreamEvent("", r); }
}
