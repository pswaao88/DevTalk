package com.devtalk.devtalk.service.llm;

import reactor.core.publisher.Flux;

public interface LlmStreamClient {
    Flux<LlmStreamEvent> stream(LlmRequest request);
}
