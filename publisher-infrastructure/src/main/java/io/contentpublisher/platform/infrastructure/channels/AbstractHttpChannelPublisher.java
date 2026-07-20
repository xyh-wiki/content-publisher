package io.contentpublisher.platform.infrastructure.channels;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.contentpublisher.platform.application.ApplicationException;
import io.contentpublisher.platform.application.port.ChannelPublisher;
import io.contentpublisher.platform.infrastructure.config.ChannelProperties;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

abstract class AbstractHttpChannelPublisher implements ChannelPublisher {
    protected final HttpClient httpClient;
    protected final ObjectMapper objectMapper;
    protected final ChannelProperties properties;

    AbstractHttpChannelPublisher(HttpClient httpClient, ObjectMapper objectMapper, ChannelProperties properties) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    protected JsonNode send(HttpRequest request) {
        if (!properties.enabled()) throw new ApplicationException("CHANNELS_DISABLED", "渠道发布功能未启用");
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String code = response.statusCode() == 429 ? "CHANNEL_RATE_LIMITED" : "CHANNEL_RESPONSE_REJECTED";
                throw new ApplicationException(code, "渠道 API 返回 HTTP " + response.statusCode());
            }
            return objectMapper.readTree(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ApplicationException("CHANNEL_REQUEST_INTERRUPTED", "渠道 API 请求被中断", exception);
        } catch (java.io.IOException exception) {
            throw new ApplicationException("CHANNEL_REQUEST_FAILED", "渠道 API 请求失败", exception);
        }
    }

    protected String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new ApplicationException("CHANNEL_REQUEST_SERIALIZATION_FAILED", "渠道请求序列化失败", exception);
        }
    }

    protected String required(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            throw new ApplicationException("CHANNEL_RESPONSE_INVALID", "渠道 API 响应缺少字段: " + field);
        }
        return value.asText();
    }
}
