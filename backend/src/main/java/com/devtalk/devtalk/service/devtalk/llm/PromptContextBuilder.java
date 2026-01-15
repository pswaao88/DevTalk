package com.devtalk.devtalk.service.devtalk.llm;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
// 상속 제한 => 이펙티브 자바 item 19
public final class PromptContextBuilder {
    // 중첩 클래스 record => 이펙티브자바 imtem 24, 모던 자바 인 액션 14장
    // record클래스는 암시적으로 static
    /** 컨텍스트 구성 정책 */
    public record Policy(
        boolean includeSystem,
        boolean includeFailed,
        boolean includeAi,
        int maxChars
    ) {
        public Policy {
            // 이펙티브 자바 item 49
            if (maxChars <= 0) throw new IllegalArgumentException("maxChars must be > 0");
        }

        /** 기본 정책 */
        public static Policy defaults(int maxChars) {
            // SYSTEM/FAILED 제외, AI 포함, 글자수 제한
            return new Policy(false, false, true, maxChars);
        }
    }

    /**
     * 도메인 모델을 직접 의존하지 않기 위한 인터페이스
     */
    public interface SourceMessage {
        LlmRole role();
        SourceStatus status();
        String content();
    }

    public enum SourceStatus { SUCCESS, FAILED }

    private final Policy policy;

    public PromptContextBuilder(Policy policy) {
        this.policy = Objects.requireNonNull(policy, "policy must not be null");
    }

    public List<LlmMessage> build(List<? extends SourceMessage> contextInOrder) {
        Objects.requireNonNull(contextInOrder, "contextInOrder must not be null");

        Deque<LlmMessage> deque = new LinkedList<>();
        int used = 0;

        // 최신부터 포함 => 메세지는 오름차순으로 정렬 과거 ... 최신
        // 최신부터 n개의 글자까지만 포함할거기 때문에 역순으로 저장
        for (int i = contextInOrder.size() - 1; i >= 0; i--) {
            SourceMessage src = contextInOrder.get(i);
            if (!shouldInclude(src)) continue;

            String content = safe(src.content());
            int add = content.length();
            if (add == 0) continue;

            if (used + add > policy.maxChars()) {
                break; // 상한 초과
            }

            // addFirst로 시간순 유지
            deque.addFirst(new LlmMessage(src.role(), content));
            used += add;
        }

        return new ArrayList<>(deque);
    }

    private boolean shouldInclude(SourceMessage m) {
        if (!policy.includeFailed() && m.status() == SourceStatus.FAILED) return false;
        if (!policy.includeSystem() && m.role() == LlmRole.SYSTEM) return false;
        if (!policy.includeAi() && m.role() == LlmRole.AI) return false;
        return true;
    }

    private static String safe(String s) {
        return (s == null) ? "" : s;
    }
}
