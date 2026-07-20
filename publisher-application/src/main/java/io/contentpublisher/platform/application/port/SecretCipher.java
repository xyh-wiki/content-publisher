package io.contentpublisher.platform.application.port;

public interface SecretCipher {
    String encrypt(String context, String plaintext);
    String decrypt(String context, String ciphertext);
    String fingerprint(String plaintext);
}
