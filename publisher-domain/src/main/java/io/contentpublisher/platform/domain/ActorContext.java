package io.contentpublisher.platform.domain;

public record ActorContext(String tenantId, String subject) {
    public ActorContext {
        tenantId = required(tenantId, "tenantId");
        subject = required(subject, "subject");
        if (!tenantId.matches("[A-Za-z0-9._-]{1,100}")) {
            throw new IllegalArgumentException("tenantId 格式无效");
        }
        if (subject.length() > 200) {
            throw new IllegalArgumentException("subject 长度不能超过 200");
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " 不能为空");
        return value.trim();
    }
}
