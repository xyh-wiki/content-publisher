package io.contentpublisher.platform.infrastructure.security;

import io.contentpublisher.platform.application.ApplicationException;
import io.contentpublisher.platform.infrastructure.config.SecretProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesGcmSecretCipherTest {
    private final AesGcmSecretCipher cipher = new AesGcmSecretCipher(
            new SecretProperties("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="));

    @Test
    void shouldEncryptApiKeyAndBindCiphertextToTenantContext() {
        String secret = "sk-enterprise-sensitive-value";
        String encrypted = cipher.encrypt("ai-api-key:tenant-a", secret);

        assertThat(encrypted).startsWith("v1:").doesNotContain(secret);
        assertThat(cipher.decrypt("ai-api-key:tenant-a", encrypted)).isEqualTo(secret);
        assertThat(cipher.fingerprint(secret)).hasSize(64);
        assertThatThrownBy(() -> cipher.decrypt("ai-api-key:tenant-b", encrypted))
                .isInstanceOf(ApplicationException.class)
                .extracting(exception -> ((ApplicationException) exception).code())
                .isEqualTo("SECRET_DECRYPTION_FAILED");
    }
}
