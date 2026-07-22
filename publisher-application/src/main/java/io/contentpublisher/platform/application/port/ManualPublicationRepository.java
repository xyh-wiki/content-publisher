package io.contentpublisher.platform.application.port;

import io.contentpublisher.platform.application.PagedResult;
import io.contentpublisher.platform.domain.ChannelType;
import io.contentpublisher.platform.domain.ManualPublication;

import java.util.List;
import java.util.UUID;

public interface ManualPublicationRepository {
    ManualPublication save(ManualPublication publication);
    List<ManualPublication> findByArticle(String tenantId, UUID articleId);
    List<ManualPublication> findByArticles(String tenantId, List<UUID> articleIds);
    List<ManualPublication> findRecent(String tenantId, int limit);
    PagedResult<ManualPublication> searchManual(String tenantId, String query, ChannelType channelType,
                                                int page, int pageSize);
}
