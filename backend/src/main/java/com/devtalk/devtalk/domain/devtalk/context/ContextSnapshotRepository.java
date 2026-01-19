package com.devtalk.devtalk.domain.devtalk.context;

public interface ContextSnapshotRepository {
    String find(String sessionId);
    String save(String sessionId, String snapshotText);
    void delete(String sessionId);
}
