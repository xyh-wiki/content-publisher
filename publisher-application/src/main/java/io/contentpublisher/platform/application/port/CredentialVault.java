package io.contentpublisher.platform.application.port;

import java.util.Map;

public interface CredentialVault {
    String encrypt(Map<String, String> credentials);
    Map<String, String> decrypt(String encryptedCredentials);
    String fingerprint(Map<String, String> credentials);
}
