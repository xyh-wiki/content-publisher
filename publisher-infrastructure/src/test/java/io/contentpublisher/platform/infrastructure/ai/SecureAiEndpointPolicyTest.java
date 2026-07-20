package io.contentpublisher.platform.infrastructure.ai;

import io.contentpublisher.platform.application.ApplicationException;
import io.contentpublisher.platform.infrastructure.config.AiEndpointSecurityProperties;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecureAiEndpointPolicyTest {
    @Test
    void shouldRejectPlainHttpCredentialsAndPrivateAddresses() {
        SecureAiEndpointPolicy policy = new SecureAiEndpointPolicy(
                new AiEndpointSecurityProperties(Set.of(), false));

        assertCode(policy, "http://api.example.com/v1", "AI_ENDPOINT_REJECTED");
        assertCode(policy, "https://user:secret@api.example.com/v1", "AI_ENDPOINT_REJECTED");
        assertCode(policy, "https://127.0.0.1:9443/v1", "AI_ADDRESS_REJECTED");
    }

    @Test
    void shouldEnforceServerSideHostAllowlist() {
        SecureAiEndpointPolicy policy = new SecureAiEndpointPolicy(
                new AiEndpointSecurityProperties(Set.of("approved.example.com"), true));

        assertCode(policy, "https://other.example.com/v1", "AI_HOST_REJECTED");
    }

    private void assertCode(SecureAiEndpointPolicy policy, String url, String code) {
        assertThatThrownBy(() -> policy.validateAndNormalize(url))
                .isInstanceOf(ApplicationException.class)
                .extracting(exception -> ((ApplicationException) exception).code())
                .isEqualTo(code);
    }
}
