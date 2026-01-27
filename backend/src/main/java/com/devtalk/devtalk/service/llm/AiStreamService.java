package com.devtalk.devtalk.service.llm;

import com.devtalk.devtalk.domain.message.Message;
import com.devtalk.devtalk.domain.message.MessageMarkers;
import com.devtalk.devtalk.domain.message.MessageRepository;
import com.devtalk.devtalk.domain.message.MessageRole;
import com.devtalk.devtalk.domain.message.MessageStatus;
import com.devtalk.devtalk.domain.llm.LlmFinishReason;
import com.devtalk.devtalk.domain.llm.LlmMessage;
import com.devtalk.devtalk.domain.llm.LlmOptions;
import com.devtalk.devtalk.service.llm.context.LlmPromptComposer;
import com.devtalk.devtalk.domain.llm.LlmRequest;
import com.devtalk.devtalk.domain.llm.LlmStreamClient;
import com.devtalk.devtalk.service.llm.context.SessionSummaryService;
import com.devtalk.devtalk.domain.llm.context.SessionSummaryStore;
import com.devtalk.devtalk.domain.llm.context.TailSelector;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

@Service
public class AiStreamService {
    private final MessageRepository messageRepository;
    private final LlmStreamClient llmStreamClient;

    private final TailSelector tailSelector;
    private final SessionSummaryStore sessionSummaryStore;
    private final SessionSummaryService sessionSummaryService;
    private final LlmPromptComposer promptComposer;

    private final TaskExecutor taskExecutor;

    private static final int MAX_CONTINUE = 2;
    private static final int ANCHOR_CHARS = 200;

    private static final String BASE_SYSTEM_PROMPT = """
        너는 DevTalk의 AI 응답자다.
        - Resolved를 판단하거나 변경하지 마라
        - 사실과 추론을 구분해라
        - 모르면 모른다고 말해라
        """;

    public AiStreamService(MessageRepository messageRepository, LlmStreamClient llmStreamClient, TailSelector tailSelector, SessionSummaryService sessionSummaryService, SessionSummaryStore sessionSummaryStore, LlmPromptComposer promptComposer, TaskExecutor taskExecutor) {
        this.messageRepository = Objects.requireNonNull(messageRepository);
        this.llmStreamClient = Objects.requireNonNull(llmStreamClient);
        this.tailSelector = Objects.requireNonNull(tailSelector);
        this.sessionSummaryService = Objects.requireNonNull(sessionSummaryService);
        this.sessionSummaryStore = Objects.requireNonNull(sessionSummaryStore);
        this.promptComposer = Objects.requireNonNull(promptComposer);
        this.taskExecutor = Objects.requireNonNull(taskExecutor);
    }

    public void streamAi(String sessionId, String replyToUserMessageId, SseEmitter emitter) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(emitter, "emitter must not be null");

        taskExecutor.execute(() -> doStream(sessionId, replyToUserMessageId, emitter));
    }

    private void doStream(String sessionId, String replyToUserMessageId, SseEmitter emitter) {
        AtomicBoolean clientGone = new AtomicBoolean(false);

        try {
            // 1) 히스토리
            List<Message> history = messageRepository.findAllBySessionId(sessionId);

            // 2) 기준 USER 메시지 결정
            Optional<Message> latestUserOpt = (replyToUserMessageId != null && !replyToUserMessageId.isBlank())
                ? findUserById(history, replyToUserMessageId)
                : findLatestSuccessUser(history);

            if (latestUserOpt.isEmpty()) {
                sendEvent(emitter, "error", "latest_user_not_found");
                emitter.complete();
                return;
            }
            Message latestUser = latestUserOpt.get();

            // 3) 요약 갱신 + tail + summary
            sessionSummaryService.updateIfNeeded(sessionId);
            List<Message> tail = tailSelector.selectTail(history, latestUser);
            String summary = sessionSummaryStore.getState(sessionId).summaryText();

            // 4) base prompt 구성
            LlmPromptComposer.ComposedPrompt composed = promptComposer.compose(summary, tail, latestUser);
            String systemPrompt = BASE_SYSTEM_PROMPT + "\n" + composed.systemPrompt();
            List<LlmMessage> baseContext = composed.messages();
            LlmOptions options = LlmOptions.defaults();

            // 5) start
            sendEvent(emitter, "start", "ok");

            // 6) 스트리밍 + 자동 이어쓰기
            StringBuilder total = new StringBuilder();
            LlmFinishReason finalReason = LlmFinishReason.UNKNOWN;
            int continueCount = 0;

            while (true) {
                LlmRequest req;

                if (continueCount == 0) {
                    req = new LlmRequest(systemPrompt, baseContext, options);
                } else {
                    String anchor = lastN(total.toString(), ANCHOR_CHARS);

                    LlmPromptComposer.ComposedPrompt cont = promptComposer.composeContinue(
                        summary,
                        tail,
                        latestUser,
                        total.toString(),
                        anchor
                    );

                    String contSystem = BASE_SYSTEM_PROMPT + "\n" + cont.systemPrompt();
                    req = new LlmRequest(contSystem, cont.messages(), options);
                }

                LlmFinishReason r = streamOnce(req, emitter, clientGone, total);
                finalReason = r;

                if (clientGone.get()) break;

                if (r == LlmFinishReason.MAX_TOKENS && continueCount < MAX_CONTINUE) {
                    continueCount++;
                    continue;
                }
                break;
            }

            // 7) 완료 시 AI 메시지 1건 저장
            Message ai = new Message(
                MessageRole.AI,
                total.toString(),
                (MessageMarkers) null,
                MessageStatus.SUCCESS
            );
            Message saved = messageRepository.append(sessionId, ai);

            if (!clientGone.get()) {
                try {
                    sendEvent(emitter, "done", jsonDone(safeId(saved), finalReason));
                } catch (Exception ignore) {}
                try { emitter.complete(); } catch (Exception ignore) {}
            } else {
                // 클라이언트가 나갔어도 서버 결과는 저장(정석)
                try { emitter.complete(); } catch (Exception ignore) {}
            }

        } catch (Exception e) {
            try { sendEvent(emitter, "error", "server_error"); } catch (Exception ignore) {}
            try { emitter.completeWithError(e); } catch (Exception ignore) {}
        }
    }

    // 1회 스트림을 끝까지 돌리고 finishReason 리턴
    private LlmFinishReason streamOnce(
        LlmRequest req,
        SseEmitter emitter,
        AtomicBoolean clientGone,
        StringBuilder total
    ) {
        final LlmFinishReason[] reason = new LlmFinishReason[]{LlmFinishReason.UNKNOWN};
        final CountDownLatch latch = new CountDownLatch(1);
        final Throwable[] errHolder = new Throwable[]{null};

        Disposable sub = llmStreamClient.stream(req).subscribe(
            evt -> {
                if (evt.delta() != null && !evt.delta().isEmpty()) {
                    total.append(evt.delta());
                    try {
                        sendEvent(emitter, "delta", evt.delta());
                    } catch (Exception io) {
                        clientGone.set(true);
                    }
                }
                if (evt.finishReason() != null && evt.finishReason() != LlmFinishReason.UNKNOWN) {
                    reason[0] = evt.finishReason();
                }
            },
            err -> {
                errHolder[0] = err;
                latch.countDown();
            },
            latch::countDown
        );

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            sub.dispose();
            throw new RuntimeException(e);
        } finally {
            sub.dispose();
        }

        if (errHolder[0] != null) {
            if (!clientGone.get()) {
                try { sendEvent(emitter, "error", "llm_stream_failed"); } catch (Exception ignore) {}
                try { emitter.completeWithError(errHolder[0]); } catch (Exception ignore) {}
            } else {
                try { emitter.complete(); } catch (Exception ignore) {}
            }
            throw new RuntimeException(errHolder[0]);
        }

        return reason[0];
    }

    private static void sendEvent(SseEmitter emitter, String name, String data) throws IOException {
        emitter.send(SseEmitter.event().name(name).data(data));
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

    private Optional<Message> findUserById(List<Message> historyInOrder, String userMessageId) {
        for (Message m : historyInOrder) {
            if (m.getRole() == MessageRole.USER && userMessageId.equals(safeId(m))) {
                return Optional.of(m);
            }
        }
        return Optional.empty();
    }

    private static String safeId(Message m) {
        try { return m.getMessageId(); } catch (Exception e) { return ""; }
    }

    private static String lastN(String s, int n) {
        if (s == null) return "";
        if (s.length() <= n) return s;
        return s.substring(s.length() - n);
    }

    private static String jsonDone(String messageId, LlmFinishReason reason) {
        String id = (messageId == null) ? "" : messageId.replace("\"", "");
        String r = (reason == null) ? "UNKNOWN" : reason.name();
        return "{\"messageId\":\"" + id + "\",\"finishReason\":\"" + r + "\"}";
    }
}
