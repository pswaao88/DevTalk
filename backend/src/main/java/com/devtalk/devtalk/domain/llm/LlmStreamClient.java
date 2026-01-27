package com.devtalk.devtalk.domain.llm;

import reactor.core.publisher.Flux;

public interface LlmStreamClient {
    Flux<LlmStreamEvent> stream(LlmRequest request);
}
