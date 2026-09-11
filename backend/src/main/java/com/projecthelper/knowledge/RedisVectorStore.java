package com.projecthelper.knowledge;

import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.output.NestedMultiOutput;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.connection.DecoratedRedisConnection;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.lettuce.LettuceConnection;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisVectorStore {
    private static final String INDEX = "knowledge_public";
    private static final String CHUNK_PREFIX = "kb:chunk:";
    private static final String DOCUMENT_SET_PREFIX = "kb:document:";

    private final StringRedisTemplate redisTemplate;
    private final KnowledgeProperties properties;
    private final EmbeddingClient embeddingClient;

    @PostConstruct
    public void initializeIndex() {
        // 应用启动时确保 Redis Stack 的向量索引存在；已存在时仅补充兼容字段。
        try {
            executeMulti("FT.INFO", bytes(INDEX));
        } catch (Exception ignored) {
            try {
                execute("FT.CREATE",
                        bytes(INDEX), bytes("ON"), bytes("HASH"), bytes("PREFIX"), bytes("1"), bytes(CHUNK_PREFIX),
                        bytes("SCHEMA"), bytes("documentId"), bytes("TAG"), bytes("title"), bytes("TEXT"),
                        bytes("originalFilename"), bytes("TEXT"), bytes("content"), bytes("TEXT"),
                        bytes("chunkIndex"), bytes("NUMERIC"), bytes("pageStart"), bytes("NUMERIC"),
                        bytes("pageEnd"), bytes("NUMERIC"),
                        bytes("contentVector"), bytes("VECTOR"), bytes("FLAT"), bytes("6"),
                        bytes("TYPE"), bytes("FLOAT32"), bytes("DIM"), bytes(String.valueOf(properties.getEmbeddingDimension())),
                        bytes("DISTANCE_METRIC"), bytes("COSINE"));
                log.info("Created RediSearch index {}", INDEX);
            } catch (Exception exception) {
                log.warn("Redis vector index is unavailable: {}", exception.getMessage());
            }
        }
        addSchemaField("originalFilename", "TEXT");
        addSchemaField("pageStart", "NUMERIC");
        addSchemaField("pageEnd", "NUMERIC");
    }

    public int replaceDocument(String documentId, String title, String originalFilename,
                               List<KnowledgeChunk> chunks) {
        // 重建文档采用“先删后写”，并用集合记录 chunk key，便于按文档完整清理。
        deleteDocument(documentId);
        String setKey = documentSetKey(documentId);
        for (KnowledgeChunk chunk : chunks) {
            String chunkId = UUID.randomUUID().toString();
            String redisKey = CHUNK_PREFIX + chunkId;
            float[] vector = embeddingClient.embed(chunk.content());
            Map<byte[], byte[]> fields = new HashMap<>();
            fields.put(bytes("documentId"), bytes(documentId));
            fields.put(bytes("title"), bytes(title));
            fields.put(bytes("originalFilename"), bytes(originalFilename == null ? title : originalFilename));
            fields.put(bytes("content"), bytes(chunk.content()));
            fields.put(bytes("chunkIndex"), bytes(String.valueOf(chunk.chunkIndex())));
            fields.put(bytes("pageStart"), bytes(String.valueOf(chunk.pageStart())));
            fields.put(bytes("pageEnd"), bytes(String.valueOf(chunk.pageEnd())));
            fields.put(bytes("contentVector"), vectorBytes(vector));
            redisTemplate.execute((RedisCallback<Void>) connection -> {
                connection.hashCommands().hMSet(bytes(redisKey), fields);
                return null;
            });
            redisTemplate.opsForSet().add(setKey, redisKey);
        }
        return chunks.size();
    }

    public void deleteDocument(String documentId) {
        String setKey = documentSetKey(documentId);
        Set<String> keys = redisTemplate.opsForSet().members(setKey);
        if (keys != null && !keys.isEmpty()) redisTemplate.delete(keys);
        redisTemplate.delete(setKey);
    }

    public List<SearchHit> search(String question, int topK) {
        // 查询文本先转向量，再通过 RediSearch KNN 返回最相近的公开文档片段。
        float[] queryVector = embeddingClient.embed(question);
        Object raw = executeMulti("FT.SEARCH", bytes(INDEX),
                bytes("*=>[KNN " + topK + " @contentVector $vector AS score]"),
                bytes("PARAMS"), bytes("2"), bytes("vector"), vectorBytes(queryVector),
                bytes("SORTBY"), bytes("score"), bytes("RETURN"), bytes("7"),
                bytes("documentId"), bytes("title"), bytes("originalFilename"), bytes("content"),
                bytes("chunkIndex"), bytes("pageStart"), bytes("pageEnd"),
                bytes("DIALECT"), bytes("2"));
        return parse(raw);
    }

    private List<SearchHit> parse(Object raw) {
        if (!(raw instanceof List<?> list)) return List.of();
        Object resultsObject = field(list, "results");
        if (resultsObject instanceof List<?> results) {
            List<SearchHit> hits = new ArrayList<>();
            for (Object resultObject : results) {
                if (!(resultObject instanceof List<?> result)) continue;
                Object attributesObject = field(result, "extra_attributes");
                if (attributesObject instanceof List<?> attributes) addHit(hits, attributes);
            }
            return hits;
        }
        if (list.size() < 3) return List.of();
        List<SearchHit> hits = new ArrayList<>();
        for (int i = 1; i + 1 < list.size(); i += 2) {
            Object fieldsObject = list.get(i + 1);
            if (!(fieldsObject instanceof List<?> fields)) continue;
            addHit(hits, fields);
        }
        return hits;
    }

    private void addHit(List<SearchHit> hits, List<?> fields) {
        Map<String, String> values = new HashMap<>();
        for (int i = 0; i + 1 < fields.size(); i += 2) {
            values.put(asString(fields.get(i)), asString(fields.get(i + 1)));
        }
        if (values.get("documentId") != null && values.get("content") != null) {
            hits.add(new SearchHit(values.get("documentId"), values.getOrDefault("title", ""),
                    values.getOrDefault("originalFilename", ""), values.get("content"),
                    Integer.parseInt(values.getOrDefault("chunkIndex", "0")),
                    Integer.parseInt(values.getOrDefault("pageStart", "0")),
                    Integer.parseInt(values.getOrDefault("pageEnd", values.getOrDefault("pageStart", "0")))));
        }
    }

    private Object field(List<?> fields, String name) {
        for (int i = 0; i + 1 < fields.size(); i += 2) {
            if (name.equals(asString(fields.get(i)))) return fields.get(i + 1);
        }
        return null;
    }

    private Object execute(String command, byte[]... args) {
        return redisTemplate.execute((RedisCallback<Object>) connection -> connection.execute(command, args));
    }

    private Object executeMulti(String command, byte[]... args) {
        return redisTemplate.execute((RedisCallback<Object>) connection -> lettuce(connection)
                .execute(command, new NestedMultiOutput<>(ByteArrayCodec.INSTANCE), args));
    }

    private void addSchemaField(String field, String type) {
        try {
            execute("FT.ALTER", bytes(INDEX), bytes("SCHEMA"), bytes("ADD"), bytes(field), bytes(type));
        } catch (Exception ignored) {
            // The field may already exist on an upgraded index.
        }
    }

    private LettuceConnection lettuce(RedisConnection connection) {
        RedisConnection current = connection;
        while (current instanceof DecoratedRedisConnection decorated) current = decorated.getDelegate();
        if (current instanceof LettuceConnection lettuce) return lettuce;
        throw new IllegalStateException("RediSearch requires a Lettuce Redis connection");
    }

    private byte[] vectorBytes(float[] vector) {
        ByteBuffer buffer = ByteBuffer.allocate(vector.length * Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for (float value : vector) buffer.putFloat(value);
        return buffer.array();
    }

    private byte[] bytes(String value) { return value.getBytes(StandardCharsets.UTF_8); }
    private String asString(Object value) {
        if (value instanceof byte[] bytes) return new String(bytes, StandardCharsets.UTF_8);
        return String.valueOf(value);
    }
    private String documentSetKey(String documentId) { return DOCUMENT_SET_PREFIX + documentId + ":chunks"; }

    public record SearchHit(String documentId, String title, String originalFilename, String content,
                            int chunkIndex, int pageStart, int pageEnd) {}
}
