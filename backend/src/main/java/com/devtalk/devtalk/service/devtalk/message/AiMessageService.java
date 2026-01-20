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
    private final LlmPromptComposer promptComposer;

    public AiMessageService(MessageRepository messageRepository, LlmClient llmClient, TailSelector tailSelector, SessionSummaryStore sessionSummaryStore, LlmPromptComposer promptComposer) {
        this.messageRepository = Objects.requireNonNull(messageRepository);
        this.llmClient = Objects.requireNonNull(llmClient);
        this.tailSelector = tailSelector;
        this.sessionSummaryStore = sessionSummaryStore;
        this.promptComposer = promptComposer;
    }

    // 대화 컨텍스트 8000자로 유지해 전달하여 응답을 얻어냄
    public MessageResponse generateAndSave(String sessionId) {
        // 1) 해당 세션의 전체 대화 내역 즉 로그를 가져옴
        List<Message> history = messageRepository.findAllBySessionId(sessionId);

        Message latestUser = findLatestSuccessUser(history)
            .orElseGet(() -> {
                // 질문이 없으면 실패도 기록 자산
                Message failed = new Message(
                    MessageRole.AI,
                    "AI 응답 생성에 실패했습니다. 최신 USER 메시지를 찾을 수 없습니다.",
                    null,
                    MessageStatus.FAILED
                );
                return messageRepository.append(sessionId, failed);
            });
        if (latestUser.getRole() != MessageRole.USER) {
            return MessageResponse.from(latestUser);
        }

        // 3) Tail 선택 (SYSTEM 제외, FAILED 제외, AI 포함, latestUser 제외)
        List<Message> tail = tailSelector.selectTail(history, latestUser);

        // 4) prefixSummary 조회 (없으면 기본 문구)
        String prefixSummary = sessionSummaryStore.getPrefixSummary(sessionId);

        // 5) Prompt 조립: systemPrompt(SUMMARY) + messages(tail -> latestUser)
        LlmPromptComposer.ComposedPrompt composed =
            promptComposer.compose(prefixSummary, tail, latestUser);

        // 6) DevTalk 고정 systemPrompt + SUMMARY 결합
        String baseSystemPrompt = """
                너는 DevTalk의 AI 응답자다.
                - Resolved를 판단하거나 변경하지 마라
                - 사실과 추론을 구분해라
                - 모르면 모른다고 말해라
                """;

        String systemPrompt = baseSystemPrompt + "\n" + composed.systemPrompt();
        List<LlmMessage> context = composed.messages();

        // 7) LLM 호출
        LlmRequest request = new LlmRequest(systemPrompt, context, LlmOptions.defaults());
        LlmResult result = llmClient.generate(request);

        // 8) 결과를 Message로 저장
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
