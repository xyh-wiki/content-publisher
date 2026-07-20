package io.contentpublisher.platform.infrastructure.channels;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.contentpublisher.platform.application.ApplicationException;
import io.contentpublisher.platform.application.port.CredentialVault;
import io.contentpublisher.platform.infrastructure.config.ChannelProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.HexFormat;

@Component
public class AesGcmCredentialVault implements CredentialVault {
    private static final TypeReference<Map<String, String>> MAP_TYPE = new TypeReference<>() {};
    private final ObjectMapper objectMapper;
    private final ChannelProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcmCredentialVault(ObjectMapper objectMapper, ChannelProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public String encrypt(Map<String, String> credentials) {
        try {
            byte[] iv = new byte[12];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            cipher.updateAAD("content-publisher:channel-credentials:v1".getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = cipher.doFinal(objectMapper.writeValueAsBytes(credentials));
            byte[] payload = java.nio.ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array();
            return "v1:" + Base64.getEncoder().encodeToString(payload);
        } catch (ApplicationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApplicationException("CREDENTIAL_ENCRYPTION_FAILED", "渠道凭据加密失败", exception);
        }
    }

    @Override
    public Map<String, String> decrypt(String encryptedCredentials) {
        try {
            if (encryptedCredentials == null || !encryptedCredentials.startsWith("v1:")) {
                throw new ApplicationException("CREDENTIAL_FORMAT_INVALID", "渠道凭据密文格式无效");
            }
            byte[] payload = Base64.getDecoder().decode(encryptedCredentials.substring(3));
            if (payload.length < 29) throw new ApplicationException("CREDENTIAL_FORMAT_INVALID", "渠道凭据密文长度无效");
            byte[] iv = java.util.Arrays.copyOfRange(payload, 0, 12);
            byte[] encrypted = java.util.Arrays.copyOfRange(payload, 12, payload.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            cipher.updateAAD("content-publisher:channel-credentials:v1".getBytes(StandardCharsets.UTF_8));
            return Map.copyOf(objectMapper.readValue(cipher.doFinal(encrypted), MAP_TYPE));
        } catch (ApplicationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApplicationException("CREDENTIAL_DECRYPTION_FAILED", "渠道凭据解密失败", exception);
        }
    }

    @Override
    public String fingerprint(Map<String, String> credentials) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key());
            byte[] canonical = objectMapper.writeValueAsBytes(new java.util.TreeMap<>(credentials));
            return HexFormat.of().formatHex(mac.doFinal(canonical));
        } catch (ApplicationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApplicationException("CREDENTIAL_FINGERPRINT_FAILED", "渠道凭据指纹生成失败", exception);
        }
    }

    private SecretKeySpec key() {
        if (!properties.enabled()) throw new ApplicationException("CHANNELS_DISABLED", "渠道发布功能未启用");
        try {
            byte[] decoded = Base64.getDecoder().decode(properties.encryptionKey());
            if (decoded.length != 32) throw new IllegalArgumentException("key length");
            return new SecretKeySpec(decoded, "AES");
        } catch (RuntimeException exception) {
            throw new ApplicationException("CHANNEL_ENCRYPTION_KEY_INVALID",
                    "PUBLISHER_CHANNELS_ENCRYPTION_KEY 必须是 Base64 编码的 32 字节密钥");
        }
    }
}
