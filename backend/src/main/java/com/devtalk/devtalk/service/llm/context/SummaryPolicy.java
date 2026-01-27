package com.devtalk.devtalk.service.llm.context;

public record SummaryPolicy(
    int promptMaxChars,   // 프롬프트 요청시 요약본 제한 길이 1000
    int hardMaxChars,     // 실제로 서버 강제하는 제한 길이 1200
    int keepTailMessages  // 최신 tail 몇 개는 요약에 포함하지 않음
) {
    public static SummaryPolicy defaults() {
        return new SummaryPolicy(1000, 1200, 12);
    }
}
