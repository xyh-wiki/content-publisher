package io.contentpublisher.platform.application.port;

public interface AiEndpointPolicy {
    String validateAndNormalize(String baseUrl);
}
