package com.devtalk.devtalk.service.devtalk.message;

import com.devtalk.devtalk.api.dto.response.MessageResponse;
import com.devtalk.devtalk.domain.devtalk.message.Message;
import com.devtalk.devtalk.domain.devtalk.message.MessageMarkers;
import com.devtalk.devtalk.domain.devtalk.message.MessageRepository;
import com.devtalk.devtalk.domain.devtalk.message.MessageRole;
import com.devtalk.devtalk.domain.devtalk.message.MessageStatus;
import com.devtalk.devtalk.service.devtalk.llm.LlmClient;
import com.devtalk.devtalk.service.devtalk.llm.LlmFinishReason;
import com.devtalk.devtalk.service.devtalk.llm.LlmMessage;
import com.devtalk.devtalk.service.devtalk.llm.LlmOptions;
import com.devtalk.devtalk.service.devtalk.llm.LlmPromptComposer;
import com.devtalk.devtalk.service.devtalk.llm.LlmRequest;
import com.devtalk.devtalk.service.devtalk.llm.LlmResult;
import com.devtalk.devtalk.service.devtalk.llm.LlmRole;
import com.devtalk.devtalk.service.devtalk.llm.context.SessionSummaryService;
import com.devtalk.devtalk.service.devtalk.llm.context.SessionSummaryStore;
import com.devtalk.devtalk.service.devtalk.llm.context.TailSelector;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public final class AiMessageService {

    private final MessageRepository messageRepository;
    private final LlmClient llmClient;

    private final TailSelector tailSelector;
    private final SessionSummaryStore sessionSummaryStore;
    private final SessionSummaryService sessionSummaryService;
    private final LlmPromptComposer promptComposer;

    private static final String CONTINUE_PROMPT =
        "방금 답변이 길이 제한으로 끊겼습니다. 바로 이어서 계속 작성해 주세요. " +
            "이미 작성한 문장은 반복하지 말고, 중간부터 자연스럽게 이어서 작성하세요.";

    private static final int MAX_CONTINUE_ROUNDS = 2;

    public AiMessageService(MessageRepository messageRepository, LlmClient llmClient, TailSelector tailSelector, SessionSummaryService sessionSummaryService, SessionSummaryStore sessionSummaryStore, LlmPromptComposer promptComposer) {
        this.messageRepository = Objects.requireNonNull(messageRepository);
        this.llmClient = Objects.requireNonNull(llmClient);
        this.tailSelector = tailSelector;
        this.sessionSummaryService = sessionSummaryService;
        this.sessionSummaryStore = sessionSummaryStore;
        this.promptComposer = promptComposer;
    }

    public MessageResponse generateAndSave(String sessionId) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");

        // 1) 전체 히스토리 조회
        List<Message> history = messageRepository.findAllBySessionId(sessionId);

        // 2) 최신 SUCCESS USER 추출 (없으면 실패 기록)
        Optional<Message> latestUserOpt = findLatestSuccessUser(history);
        if (latestUserOpt.isEmpty()) {
            Message failed = new Message(
                MessageRole.AI,
                "AI 응답 생성에 실패했습니다. 최신 USER 메시지를 찾을 수 없습니다.",
                null,
                MessageStatus.FAILED
            );
            return MessageResponse.from(messageRepository.append(sessionId, failed));
        }
        Message latestUser = latestUserOpt.get();

        // 3) 요약 갱신 (필요한 경우에만 내부에서 수행)
        //    - 실패해도 내부에서 SYSTEM FAILED 로깅하고 계속 진행(요약은 기존 상태 유지)
        sessionSummaryService.updateIfNeeded(sessionId);

        // 4) Tail 선택 (SYSTEM 제외, FAILED 제외, AI 포함, latestUser 제외)
        List<Message> tail = tailSelector.selectTail(history, latestUser);

        // 5) 요약 조회 (없으면 기본 문구)
        String summary = sessionSummaryStore.getState(sessionId).summaryText();

        // 6) Prompt 조립: SUMMARY(system_prompt) + messages(tail -> latestUser)
        LlmPromptComposer.ComposedPrompt composed =
            promptComposer.compose(summary, tail, latestUser);

        // 7) DevTalk 고정 systemPrompt + SUMMARY 결합
        String baseSystemPrompt = """
                너는 DevTalk의 AI 응답자다.
                - Resolved를 판단하거나 변경하지 마라
                - 사실과 추론을 구분해라
                - 모르면 모른다고 말해라
                """;

        String systemPrompt = baseSystemPrompt + "\n" + composed.systemPrompt();
        List<LlmMessage> baseContext = composed.messages();

        LlmOptions options = LlmOptions.defaults();

        // 8) LLM 호출 (자동 이어쓰기 포함)
        StringBuilder total = new StringBuilder();
        LlmFinishReason lastReason = LlmFinishReason.UNKNOWN;

        int maxRounds = 1 + MAX_CONTINUE_ROUNDS;
        for (int round = 1; round <= maxRounds; round++) {

            List<LlmMessage> ctx = (round == 1)
                ? baseContext
                : buildContinueContext(baseContext, total.toString());

            LlmRequest request = new LlmRequest(systemPrompt, ctx, options);
            LlmResult result = llmClient.generate(request);

            if (result instanceof LlmResult.Failure f) {
                Message failed = new Message(
                    MessageRole.AI,
                    "AI 응답 생성에 실패했습니다. 잠시 후 다시 시도해주세요."
                        + "\n(code=" + f.code() + ")",
                    null,
                    MessageStatus.FAILED
                );
                return MessageResponse.from(messageRepository.append(sessionId, failed));
            }

            LlmResult.Success s = (LlmResult.Success) result;
            total.append(s.text());
            lastReason = s.finishReason();

            // ✅ 토큰 때문에 끊긴 경우에만 이어쓰기
            if (lastReason != LlmFinishReason.MAX_TOKENS) {
                break;
            }
        }

        // 9) 최종 결과 저장 (유저 입장에서 1개의 자연스러운 답변)
        Message aiMessage = new Message(
            MessageRole.AI,
            total.toString(),
            (MessageMarkers) null,
            MessageStatus.SUCCESS
        );

        return MessageResponse.from(messageRepository.append(sessionId, aiMessage));
    }

    private List<LlmMessage> buildContinueContext(List<LlmMessage> baseContext, String assistantSoFar) {
        List<LlmMessage> merged = new ArrayList<>(baseContext.size() + 2);
        merged.addAll(baseContext);

        // “지금까지 AI가 말한 내용”을 넣고,
        merged.add(new LlmMessage(LlmRole.AI, assistantSoFar));

        // “이어서 계속”을 user로 넣는다
        merged.add(new LlmMessage(LlmRole.USER, CONTINUE_PROMPT));

        return merged;
    }

    private Optional<Message> findLatestSuccessUser(List<Message> historyInOrder) {
        for (int i = historyInOrder.size() - 1; i >= 0; i--) {
            Message m = historyInOrder.get(i);
            if (m.getRole() == MessageRole.USER && m.getStatus() == MessageStatus.SUCCESS) {
                return Optional.of(m);
            }
        }
        return Optional.empty();
    }

}
