package com.devtalk.devtalk.domain.llm;

public enum LlmFailureCode {
    NETWORK_ERROR,
    AUTH_ERROR,
    RATE_LIMIT,
    INVALID_REQUEST,
    PROVIDER_ERROR,
    TIMEOUT,
    UNKNOWN
}
