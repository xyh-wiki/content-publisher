package io.contentpublisher.platform.infrastructure.channels;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.contentpublisher.platform.infrastructure.config.ChannelProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesGcmCredentialVaultTest {
    @Test
    void shouldEncryptWithRandomIvAndDecryptCredentials() {
        String key = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());
        var vault = new AesGcmCredentialVault(new ObjectMapper(),
                new ChannelProperties(true, key, Set.of(), Duration.ofSeconds(5)));
        Map<String, String> credentials = Map.of("apiKey", "secret-value");

        String first = vault.encrypt(credentials);
        String second = vault.encrypt(credentials);

        assertThat(first).startsWith("v1:").isNotEqualTo(second).doesNotContain("secret-value");
        assertThat(vault.decrypt(first)).isEqualTo(credentials);
        assertThat(vault.fingerprint(credentials)).hasSize(64).doesNotContain("secret-value")
                .isEqualTo(vault.fingerprint(credentials));
    }

    @Test
    void shouldRejectInvalidMasterKey() {
        var vault = new AesGcmCredentialVault(new ObjectMapper(),
                new ChannelProperties(true, "invalid", Set.of(), Duration.ofSeconds(5)));

        assertThatThrownBy(() -> vault.encrypt(Map.of("apiKey", "secret")))
                .hasMessageContaining("32 字节密钥");
    }
}
