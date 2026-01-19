package com.devtalk.devtalk.infra.persistence;

import com.devtalk.devtalk.domain.devtalk.context.ContextSnapshotRepository;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryContextSnapshotRepository implements ContextSnapshotRepository {
    private final ConcurrentMap<String, String> contextSnapshotBySession = new ConcurrentHashMap<>();

    @Override
    public String find(String sessionId){
        return contextSnapshotBySession.get(sessionId);
    }

    @Override
    public String save(String sessionId, String snapshotText){
        contextSnapshotBySession.put(sessionId, snapshotText);
        return snapshotText;
    }

    @Override
    public void delete(String sessionId){
        contextSnapshotBySession.remove(sessionId);
    }

}
