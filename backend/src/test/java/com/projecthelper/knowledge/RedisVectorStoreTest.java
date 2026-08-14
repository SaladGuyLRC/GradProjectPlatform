package com.projecthelper.knowledge;

import io.lettuce.core.output.NestedMultiOutput;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.DefaultStringRedisConnection;
import org.springframework.data.redis.connection.lettuce.LettuceConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisVectorStoreTest {
    @Test
    void searchUsesNestedOutputForRediSearchResults() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        LettuceConnection lettuce = mock(LettuceConnection.class);
        DefaultStringRedisConnection decorated = new DefaultStringRedisConnection(lettuce);
        EmbeddingClient embeddingClient = mock(EmbeddingClient.class);
        KnowledgeProperties properties = new KnowledgeProperties();
        properties.setEmbeddingDimension(2);
        when(embeddingClient.embed("question")).thenReturn(new float[]{0.1f, 0.2f});
        List<Object> raw = List.of(
                bytes("attributes"), List.of(),
                bytes("format"), bytes("STRING"),
                bytes("results"), List.of(List.of(
                        bytes("id"), bytes("kb:chunk:1"),
                        bytes("extra_attributes"), List.of(
                                bytes("documentId"), bytes("doc-1"),
                                bytes("title"), bytes("Guide"),
                                bytes("content"), bytes("Answer"),
                                bytes("chunkIndex"), bytes("2")),
                        bytes("values"), List.of())),
                bytes("total_results"), 1L,
                bytes("warning"), List.of());
        when(lettuce.execute(anyString(), any(NestedMultiOutput.class), any(byte[][].class))).thenReturn(raw);
        when(redisTemplate.execute(any(RedisCallback.class))).thenAnswer(invocation ->
                ((RedisCallback<?>) invocation.getArgument(0)).doInRedis(decorated));
        RedisVectorStore store = new RedisVectorStore(redisTemplate, properties, embeddingClient);

        var hits = store.search("question", 3);

        assertEquals(List.of(new RedisVectorStore.SearchHit("doc-1", "Guide", "Answer", 2)), hits);
        verify(lettuce).execute(anyString(), any(NestedMultiOutput.class), any(byte[][].class));
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
