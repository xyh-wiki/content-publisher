package io.contentpublisher.platform.web.error;

import java.time.Instant;
import java.util.List;

public record ApiError(Instant timestamp, int status, String code, String message, String path,
                       String traceId, List<FieldViolation> violations) {
    public record FieldViolation(String field, String message) {}
}
