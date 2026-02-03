package com.devtalk.devtalk.infra.llm;

import com.devtalk.devtalk.domain.llm.LlmClient;
import com.devtalk.devtalk.domain.llm.LlmFailureCode;
import com.devtalk.devtalk.domain.llm.LlmFinishReason;
import com.devtalk.devtalk.domain.llm.LlmRequest;
import com.devtalk.devtalk.domain.llm.LlmResult;
import com.devtalk.devtalk.domain.llm.LlmTokenUsage;
import com.devtalk.devtalk.domain.message.MessageMetadata;
import java.time.Instant;

public final class MockLlmClient implements LlmClient {

    private final boolean alwaysFail;
    private int callCount = 0;

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

        callCount++;

        // ---- 1️⃣ 첫 호출: 일부러 토큰 잘림 흉내 ----
        if (callCount == 1) {
            String text =
                "(MOCK AI PART 1)\n" +
                    "now=" + Instant.now() + "\n" +
                    "이 답변은 일부러 중간에서 끊깁니다...\n";

            return new LlmResult.Success(text, LlmFinishReason.MAX_TOKENS, LlmTokenUsage.empty());
        }

        // ---- 2️⃣ 이어쓰기 호출: 정상 종료 ----
        String text =
            "(MOCK AI PART 2 - CONTINUE)\n" +
                "이어서 계속 작성된 내용입니다.\n" +
                "messages=" + request.messages().size();

        return new LlmResult.Success(text, LlmFinishReason.STOP, LlmTokenUsage.empty());
    }
}
