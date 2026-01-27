package com.devtalk.devtalk.service.llm.context;

public record TailSelectorPolicy(
    int maxMessages,
    int maxChars
) {
    public static TailSelectorPolicy defaultPolicy() {
        return new TailSelectorPolicy(12, 6000);
    }
}
