package io.contentpublisher.platform.application.port;

import io.contentpublisher.platform.domain.Publication;

import java.util.Optional;
import java.util.UUID;

public interface PublicationRepository {
    Publication save(Publication publication);
    Optional<Publication> findPublicationById(String tenantId, UUID id);
    Optional<Publication> findByPublicationJobId(String tenantId, UUID jobId);
}
