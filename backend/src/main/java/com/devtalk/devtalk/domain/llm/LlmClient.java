package com.devtalk.devtalk.domain.llm;

public interface LlmClient {
    LlmResult generate(LlmRequest request);
}
