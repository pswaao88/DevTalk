package com.devtalk.devtalk.service.llm;

public interface LlmClient {
    LlmResult generate(LlmRequest request);
}
