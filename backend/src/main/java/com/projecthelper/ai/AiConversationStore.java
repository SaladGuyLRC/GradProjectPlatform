package com.projecthelper.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiConversationStore {
    private static final int MAX_MESSAGES = 20;
    private static final Duration TTL = Duration.ofHours(2);
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public List<ConversationMessage> load(String userId, String conversationId) {
        // 会话键包含用户 ID，消息列表限制为最近 20 条并由 Redis TTL 自动过期。
        List<String> values = redis.opsForList().range(key(userId, conversationId), 0, -1);
        if (values == null) return List.of();
        List<ConversationMessage> messages = new ArrayList<>();
        for (String value : values) {
            try { messages.add(mapper.readValue(value, ConversationMessage.class)); }
            catch (JsonProcessingException ignored) { /* Ignore stale entries. */ }
        }
        return messages;
    }

    public void append(String userId, String conversationId, ConversationMessage message) {
        // 追加后裁剪历史并刷新两小时 TTL，形成简单的滑动窗口记忆。
        try {
            String redisKey = key(userId, conversationId);
            redis.opsForList().rightPush(redisKey, mapper.writeValueAsString(message));
            redis.opsForList().trim(redisKey, -MAX_MESSAGES, -1);
            redis.expire(redisKey, TTL);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to save AI conversation", exception);
        }
    }

    private String key(String userId, String conversationId) {
        return "ai:conversation:" + userId + ":" + conversationId;
    }

    public record ConversationMessage(String role, String content) {}
}
