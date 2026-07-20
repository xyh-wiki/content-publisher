package io.contentpublisher.platform.application;

import java.time.Instant;
import java.util.UUID;

public record DeletedRecord(UUID id, String recordType, String title, String status,
                            Instant deletedAt, String deletedBy) {
}
