package io.contentpublisher.platform.application.port;

import io.contentpublisher.platform.domain.Publication;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PublicationRepository {
    Publication save(Publication publication);
    Optional<Publication> findPublicationById(String tenantId, UUID id);
    Optional<Publication> findByPublicationJobId(String tenantId, UUID jobId);
    List<Publication> findApiByArticle(String tenantId, UUID articleId);
    List<Publication> findApiByArticles(String tenantId, List<UUID> articleIds);
    List<Publication> findRecentApi(String tenantId, int limit);
}
