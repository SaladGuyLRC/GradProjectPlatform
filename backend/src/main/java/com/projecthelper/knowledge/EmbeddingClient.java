package com.projecthelper.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class EmbeddingClient {
    private final ObjectMapper objectMapper;
    private final KnowledgeProperties properties;

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;
    @Value("${spring.ai.dashscope.base-url}")
    private String baseUrl;

    public float[] embed(String text) {
        String response = RestClient.create().post().uri(baseUrl + "/embeddings")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body(Map.of("model", properties.getEmbeddingModel(), "input", text))
                .retrieve().body(String.class);
        try {
            JsonNode vector = objectMapper.readTree(response).path("data").path(0).path("embedding");
            if (!vector.isArray()) throw new IllegalStateException("Embedding响应缺少向量");
            float[] values = new float[vector.size()];
            for (int i = 0; i < vector.size(); i++) values[i] = (float) vector.get(i).asDouble();
            if (values.length != properties.getEmbeddingDimension()) {
                throw new IllegalStateException("Embedding维度不匹配，期望" + properties.getEmbeddingDimension() + "，实际" + values.length);
            }
            return values;
        } catch (Exception exception) {
            throw new IllegalStateException("无法解析Embedding响应", exception);
        }
    }
}
