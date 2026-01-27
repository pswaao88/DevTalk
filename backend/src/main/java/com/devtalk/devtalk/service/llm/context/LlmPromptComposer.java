package com.devtalk.devtalk.service.llm.context;

import com.devtalk.devtalk.domain.llm.LlmMessage;
import com.devtalk.devtalk.domain.llm.LlmRole;
import com.devtalk.devtalk.domain.message.Message;
import com.devtalk.devtalk.domain.message.MessageRole;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class LlmPromptComposer {

    public record ComposedPrompt(String systemPrompt, List<LlmMessage> messages) {}

    /**
     * DevTalk 컨텍스트 규칙:
     * - systemPrompt: [SUMMARY] + prefixSummary (Gemini system_instruction 용)
     * - messages: tail -> latestUser
     * - role 유지 (tail: USER/AI), latestUser는 항상 USER로 마지막
     */
    public ComposedPrompt compose(String prefixSummary, List<Message> tail, Message latestUser) {
        Objects.requireNonNull(prefixSummary, "prefixSummary must not be null");
        Objects.requireNonNull(tail, "tail must not be null");
        Objects.requireNonNull(latestUser, "latestUser must not be null");

        // 1) system prompt (SUMMARY) - system_instruction로 보낼 문자열
        String systemPrompt = "[SUMMARY]\n" + prefixSummary;

        // 2) contents(messages): tail -> latestUser
        List<LlmMessage> out = new ArrayList<>(tail.size() + 1);

        for (Message m : tail) {
            out.add(toLlmMessage(m));
        }

        // latestUser는 항상 마지막 + USER role
        out.add(new LlmMessage(LlmRole.USER, safe(latestUser.getContent())));

        return new ComposedPrompt(systemPrompt, List.copyOf(out));
    }

    public ComposedPrompt composeContinue(String prefixSummary, List<Message> tail, Message latestUser, String assistantSoFar, String lastAnchor) {
        Objects.requireNonNull(prefixSummary, "prefixSummary must not be null");
        Objects.requireNonNull(tail, "tail must not be null");
        Objects.requireNonNull(latestUser, "latestUser must not be null");

        String anchor = safe(lastAnchor);

        String continueRules = """
            [CONTINUE]
            이전 답변이 길이 제한으로 끊겨 이어서 작성해야 한다.

            규칙:
            - 이미 출력된 내용을 절대 반복하지 마라.
            - '...' 같은 말줄임/머뭇거림/재시작 문구를 쓰지 마라.
            - 아래 ANCHOR 구간은 "이미 사용자에게 보여진 마지막 부분"이다.
            - 반드시 ANCHOR 바로 다음에 와야 할 "새로운 텍스트"부터 출력하라.
            - 가능한 한 자연스럽게 문장을 이어라.

            [ANCHOR]
            <<<ANCHOR_START
            %s
            ANCHOR_END>>>
            """.formatted(anchor);

        String systemPrompt = "[SUMMARY]\n" + prefixSummary + "\n\n" + continueRules;

        List<LlmMessage> out = new ArrayList<>(tail.size() + 1);
        for (Message m : tail) out.add(toLlmMessage(m));

        // latestUser는 항상 마지막 USER role 유지
        out.add(new LlmMessage(LlmRole.USER, safe(latestUser.getContent())));

        return new ComposedPrompt(systemPrompt, List.copyOf(out));
    }

    private LlmMessage toLlmMessage(Message m) {
        Objects.requireNonNull(m, "message must not be null");

        // TailSelector가 SYSTEM 제외를 보장하지만, 방어적으로 처리
        if (m.getRole() == MessageRole.SYSTEM) {
            throw new IllegalArgumentException("SYSTEM message must not be in tail");
        }

        LlmRole role = switch (m.getRole()) {
            case USER -> LlmRole.USER;
            case AI -> LlmRole.AI;
            case SYSTEM -> throw new IllegalStateException("unreachable");
        };

        return new LlmMessage(role, safe(m.getContent()));
    }

    private String safe(String s) {
        return (s == null) ? "" : s;
    }
}
