package com.devtalk.devtalk.service.devtalk.message;

import com.devtalk.devtalk.domain.devtalk.message.Message;
import com.devtalk.devtalk.domain.devtalk.message.MessageMarkers;
import com.devtalk.devtalk.domain.devtalk.message.MessageRepository;
import com.devtalk.devtalk.domain.devtalk.message.MessageRole;
import com.devtalk.devtalk.domain.devtalk.message.MessageStatus;
import com.devtalk.devtalk.service.devtalk.llm.LlmClient;
import com.devtalk.devtalk.service.devtalk.llm.LlmMessage;
import com.devtalk.devtalk.service.devtalk.llm.LlmOptions;
import com.devtalk.devtalk.service.devtalk.llm.LlmRequest;
import com.devtalk.devtalk.service.devtalk.llm.LlmResult;
import com.devtalk.devtalk.service.devtalk.llm.LlmRole;
import com.devtalk.devtalk.service.devtalk.llm.PromptContextBuilder;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public final class AiMessageService {

    private final MessageRepository messageRepository;
    private final PromptContextBuilder contextBuilder;
    private final LlmClient llmClient;

    public AiMessageService(MessageRepository messageRepository, PromptContextBuilder contextBuilder, LlmClient llmClient) {
        this.messageRepository = Objects.requireNonNull(messageRepository);
        this.contextBuilder = Objects.requireNonNull(contextBuilder);
        this.llmClient = Objects.requireNonNull(llmClient);
    }

    // 대화 컨텍스트 8000자로 유지해 전달하여 응답을 얻어냄
    public Message generateAndSave(String sessionId) {
        // 1) 해당 세션의 전체 대화 내역 즉 로그를 가져옴
        List<Message> history = messageRepository.findAllBySessionId(sessionId);

        // 2) Message -> SourceMessage 어댑터로 변환
        List<PromptContextBuilder.SourceMessage> sources = history.stream()
            .<PromptContextBuilder.SourceMessage>map(DomainMessageAdapter::new)
            .toList();

        // 3) 컨텍스트 구성
        List<LlmMessage> context = contextBuilder.build(sources);

        // 4) systemPrompt + LlmRequest 생성
        String systemPrompt = """
                너는 DevTalk의 AI 응답자다.
                - Resolved를 판단하거나 변경하지 마라
                - 사실과 추론을 구분해라
                - 모르면 모른다고 말해라
                """;

        LlmRequest request = new LlmRequest(systemPrompt, context, LlmOptions.defaults());

        // 5) LLM 호출
        LlmResult result = llmClient.generate(request);

        // 6) 결과를 Message로 저장
        String messageId = UUID.randomUUID().toString();

        // 현재는 markers에 대한 로직이 없으니 null처리 프론트에서 넘어오는 로직 완성되면 반영
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

        // repository 시그니처에 맞게 append 사용
        return messageRepository.append(sessionId, aiMessage);
    }
    // 어댑터 디자인 패턴 공부 필요
    private static final class DomainMessageAdapter implements PromptContextBuilder.SourceMessage {
        private final Message m;

        private DomainMessageAdapter(Message m) {
            this.m = m;
        }

        @Override
        public LlmRole role() {
            return switch (m.getRole()) {
                case USER -> LlmRole.USER;
                case AI -> LlmRole.AI;
                case SYSTEM -> LlmRole.SYSTEM;
            };
        }

        @Override
        public PromptContextBuilder.SourceStatus status() {
            return (m.getStatus() == MessageStatus.SUCCESS)
                ? PromptContextBuilder.SourceStatus.SUCCESS
                : PromptContextBuilder.SourceStatus.FAILED;
        }

        @Override
        public String content() {
            return m.getContent();
        }
    }
}
