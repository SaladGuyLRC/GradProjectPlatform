package com.projecthelper.ai.taskdraft;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskDraftStore {
    private static final Duration STORAGE_TTL = Duration.ofMinutes(20);
    private static final Duration CONFIRMATION_LOCK_TTL = Duration.ofSeconds(30);
    private static final DefaultRedisScript<Long> RELEASE_LOCK = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public void save(TaskDraft draft) {
        try {
            redis.opsForValue().set(key(draft.draftId()), mapper.writeValueAsString(draft), STORAGE_TTL);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to save task draft", exception);
        }
    }

    public Optional<TaskDraft> find(String draftId) {
        String value = redis.opsForValue().get(key(draftId));
        if (value == null) return Optional.empty();
        try {
            return Optional.of(mapper.readValue(value, TaskDraft.class));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to read task draft", exception);
        }
    }

    public boolean acquireConfirmationLock(String draftId, String lockToken) {
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(
                lockKey(draftId), lockToken, CONFIRMATION_LOCK_TTL));
    }

    public void releaseConfirmationLock(String draftId, String lockToken) {
        redis.execute(RELEASE_LOCK, List.of(lockKey(draftId)), lockToken);
    }

    private String key(String draftId) { return "ai:task-draft:" + draftId; }
    private String lockKey(String draftId) { return "ai:task-draft-lock:" + draftId; }
}
