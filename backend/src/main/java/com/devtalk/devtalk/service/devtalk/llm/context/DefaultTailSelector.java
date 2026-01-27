package com.devtalk.devtalk.service.devtalk.llm.context;

import com.devtalk.devtalk.domain.message.Message;
import com.devtalk.devtalk.domain.message.MessageRole;
import com.devtalk.devtalk.domain.message.MessageStatus;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

public class DefaultTailSelector implements TailSelector {

    private final TailSelectorPolicy policy;

    public DefaultTailSelector(TailSelectorPolicy policy) {
        this.policy = Objects.requireNonNull(policy, "policy must not be null");
    }

    @Override
    public List<Message> selectTail(List<Message> historyInOrder, Message latestUser) {
        Objects.requireNonNull(historyInOrder, "historyInOrder must not be null");
        Objects.requireNonNull(latestUser, "latestUser must not be null");

        int usedChars = 0;
        int usedMessages = 0;

        Deque<Message> selected = new ArrayDeque<>();

        for (int i = historyInOrder.size() - 1; i >= 0; i--) {
            Message m = historyInOrder.get(i);
            if (!shouldInclude(m, latestUser)) continue;

            int add = estimateChars(m);

            if (usedMessages + 1 > policy.maxMessages()) break;
            if (usedChars + add > policy.maxChars()) break;

            selected.addFirst(m);
            usedMessages++;
            usedChars += add;
        }

        return List.copyOf(selected);
    }

    private boolean shouldInclude(Message m, Message latestUser) {
        if (m.getRole() == MessageRole.SYSTEM) return false;                // SYSTEM 제외
        if (m.getStatus() != MessageStatus.SUCCESS) return false;           // FAILED 제외
        if (m.getMessageId().equals(latestUser.getMessageId())) return false;  // latestUser 제외
        return m.getRole() == MessageRole.USER || m.getRole() == MessageRole.AI; // AI 포함
    }

    // 컨텍스트 여유 공간 확보
    private int estimateChars(Message m) {
        String content = (m.getContent() == null) ? "" : m.getContent();
        return content.length() + 32;
    }
}
