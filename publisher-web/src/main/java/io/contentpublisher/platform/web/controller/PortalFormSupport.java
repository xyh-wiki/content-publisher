package io.contentpublisher.platform.web.controller;

import org.springframework.validation.BindingResult;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

final class PortalFormSupport {
    private PortalFormSupport() {
    }

    static List<String> splitValues(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split("[,\\n]"))
                .map(String::trim).filter(item -> !item.isBlank()).distinct().toList();
    }

    static String firstError(BindingResult bindingResult) {
        var error = bindingResult.getFieldError();
        return error == null ? "表单参数校验失败" : error.getDefaultMessage();
    }

    static String idempotencyKey(String prefix) {
        return "portal:" + prefix + ":" + UUID.randomUUID();
    }

    static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    static String value(String value) {
        return value == null ? "" : value.trim();
    }
}
