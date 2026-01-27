package com.devtalk.devtalk.config;

import com.devtalk.devtalk.infra.persistence.InMemorySessionSummaryStore;
import com.devtalk.devtalk.service.llm.LlmPromptComposer;
import com.devtalk.devtalk.service.llm.context.DefaultTailSelector;
import com.devtalk.devtalk.service.llm.context.SessionSummaryStore;
import com.devtalk.devtalk.service.llm.context.SummaryPolicy;
import com.devtalk.devtalk.service.llm.context.TailSelector;
import com.devtalk.devtalk.service.llm.context.TailSelectorPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiContextConfig {
    @Bean
    public TailSelectorPolicy tailSelectorPolicy() {
        return TailSelectorPolicy.defaultPolicy();
    }

    @Bean
    public TailSelector tailSelector(TailSelectorPolicy policy) {
        return new DefaultTailSelector(policy);
    }

    @Bean
    public SummaryPolicy summaryPolicy() {
        return SummaryPolicy.defaults();
    }

    @Bean
    public SessionSummaryStore sessionSummaryStore() {
        return new InMemorySessionSummaryStore();
    }

    @Bean
    public LlmPromptComposer llmPromptComposer() {
        return new LlmPromptComposer();
    }
}
