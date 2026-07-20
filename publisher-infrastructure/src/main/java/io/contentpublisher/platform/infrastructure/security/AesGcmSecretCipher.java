package io.contentpublisher.platform.infrastructure.security;

import io.contentpublisher.platform.application.ApplicationException;
import io.contentpublisher.platform.application.port.SecretCipher;
import io.contentpublisher.platform.infrastructure.config.SecretProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class AesGcmSecretCipher implements SecretCipher {
    private static final String VERSION = "v1:";
    private final SecretProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcmSecretCipher(SecretProperties properties) {
        this.properties = properties;
    }

    @Override
    public String encrypt(String context, String plaintext) {
        try {
            byte[] iv = new byte[12];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            cipher.updateAAD(aad(context));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return VERSION + Base64.getEncoder().encodeToString(
                    ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
        } catch (ApplicationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApplicationException("SECRET_ENCRYPTION_FAILED", "敏感配置加密失败", exception);
        }
    }

    @Override
    public String decrypt(String context, String ciphertext) {
        try {
            if (ciphertext == null || !ciphertext.startsWith(VERSION)) {
                throw new ApplicationException("SECRET_FORMAT_INVALID", "敏感配置密文格式无效");
            }
            byte[] payload = Base64.getDecoder().decode(ciphertext.substring(VERSION.length()));
            if (payload.length < 29) {
                throw new ApplicationException("SECRET_FORMAT_INVALID", "敏感配置密文长度无效");
            }
            byte[] iv = java.util.Arrays.copyOfRange(payload, 0, 12);
            byte[] encrypted = java.util.Arrays.copyOfRange(payload, 12, payload.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            cipher.updateAAD(aad(context));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (ApplicationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApplicationException("SECRET_DECRYPTION_FAILED", "敏感配置解密失败", exception);
        }
    }

    @Override
    public String fingerprint(String plaintext) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key());
            return HexFormat.of().formatHex(mac.doFinal(plaintext.getBytes(StandardCharsets.UTF_8)));
        } catch (ApplicationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApplicationException("SECRET_FINGERPRINT_FAILED", "敏感配置指纹生成失败", exception);
        }
    }

    private SecretKeySpec key() {
        try {
            byte[] decoded = Base64.getDecoder().decode(properties.encryptionKey());
            if (decoded.length != 32) throw new IllegalArgumentException("key length");
            return new SecretKeySpec(decoded, "AES");
        } catch (RuntimeException exception) {
            throw new ApplicationException("SECRET_ENCRYPTION_KEY_INVALID",
                    "PUBLISHER_SECRETS_ENCRYPTION_KEY 必须是 Base64 编码的 32 字节密钥");
        }
    }

    private byte[] aad(String context) {
        if (context == null || context.isBlank() || context.length() > 300) {
            throw new ApplicationException("SECRET_CONTEXT_INVALID", "敏感配置加密上下文无效");
        }
        return ("content-publisher:secret:v1:" + context).getBytes(StandardCharsets.UTF_8);
    }
}
