package com.devtalk.devtalk.service.devtalk.message;

import com.devtalk.devtalk.api.dto.response.MessageResponse;
import com.devtalk.devtalk.domain.devtalk.message.Message;
import com.devtalk.devtalk.domain.devtalk.message.MessageMarkers;
import com.devtalk.devtalk.domain.devtalk.message.MessageRepository;
import com.devtalk.devtalk.domain.devtalk.message.MessageRole;
import com.devtalk.devtalk.domain.devtalk.message.MessageStatus;
import com.devtalk.devtalk.service.devtalk.llm.LlmClient;
import com.devtalk.devtalk.service.devtalk.llm.LlmMessage;
import com.devtalk.devtalk.service.devtalk.llm.LlmOptions;
import com.devtalk.devtalk.service.devtalk.llm.LlmPromptComposer;
import com.devtalk.devtalk.service.devtalk.llm.LlmRequest;
import com.devtalk.devtalk.service.devtalk.llm.LlmResult;
import com.devtalk.devtalk.service.devtalk.llm.context.SessionSummaryService;
import com.devtalk.devtalk.service.devtalk.llm.context.SessionSummaryStore;
import com.devtalk.devtalk.service.devtalk.llm.context.TailSelector;
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
        List<LlmMessage> context = composed.messages();

        // 8) LLM 호출
        LlmRequest request = new LlmRequest(systemPrompt, context, LlmOptions.defaults());
        LlmResult result = llmClient.generate(request);

        // 9) 결과 저장
        MessageMarkers markers = null;

        Message aiMessage = switch (result) {
            case LlmResult.Success s -> new Message(
                MessageRole.AI,
                s.text(),
                markers,
                MessageStatus.SUCCESS
            );
            case LlmResult.Failure f -> new Message(
                MessageRole.AI,
                "AI 응답 생성에 실패했습니다. 잠시 후 다시 시도해주세요."
                    + "\n(code=" + f.code() + ")",
                markers,
                MessageStatus.FAILED
            );
        };

        return MessageResponse.from(messageRepository.append(sessionId, aiMessage));
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
