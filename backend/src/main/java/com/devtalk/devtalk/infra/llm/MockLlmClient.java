package com.devtalk.devtalk.infra.llm;

import com.devtalk.devtalk.service.devtalk.llm.LlmClient;
import com.devtalk.devtalk.service.devtalk.llm.LlmFailureCode;
import com.devtalk.devtalk.service.devtalk.llm.LlmRequest;
import com.devtalk.devtalk.service.devtalk.llm.LlmResult;
import java.time.Instant;

public final class MockLlmClient implements LlmClient {

    private final boolean alwaysFail;

    public MockLlmClient(boolean alwaysFail) {
        this.alwaysFail = alwaysFail;
    }

    @Override
    public LlmResult generate(LlmRequest request) {
        if (alwaysFail) {
            return LlmResult.Failure.of(
                LlmFailureCode.PROVIDER_ERROR,
                "Mock failure",
                "alwaysFail=true"
            );
        }

        String text = "(MOCK AI) now=" + Instant.now() + "\n"
            + "messages=" + request.messages().size();

        return new LlmResult.Success(text);
    }
}
