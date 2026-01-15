package com.devtalk.devtalk.config;

import com.devtalk.devtalk.service.devtalk.llm.PromptContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PromptContextConfig {

    @Bean
    public PromptContextBuilder.Policy promptContextPolicy(
        @Value("${devtalk.prompt.include-system:false}") boolean includeSystem,
        @Value("${devtalk.prompt.include-failed:false}") boolean includeFailed,
        @Value("${devtalk.prompt.include-ai:true}") boolean includeAi,
        @Value("${devtalk.prompt.max-chars:8000}") int maxChars
    ) {
        return new PromptContextBuilder.Policy(includeSystem, includeFailed, includeAi, maxChars);
    }

    @Bean
    public PromptContextBuilder promptContextBuilder(PromptContextBuilder.Policy policy) {
        return new PromptContextBuilder(policy);
    }
}
