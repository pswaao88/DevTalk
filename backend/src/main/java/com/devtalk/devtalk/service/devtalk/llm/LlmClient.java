package com.devtalk.devtalk.service.devtalk.llm;

public interface LlmClient {
    LlmResult generate(LlmRequest request);
}
