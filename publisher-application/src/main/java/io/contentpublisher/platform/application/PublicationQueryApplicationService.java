package io.contentpublisher.platform.application;

import io.contentpublisher.platform.application.port.ArticleRepository;
import io.contentpublisher.platform.application.port.ChannelAccountRepository;
import io.contentpublisher.platform.application.port.ManualPublicationRepository;
import io.contentpublisher.platform.application.port.PublicationRepository;
import io.contentpublisher.platform.domain.ActorContext;
import io.contentpublisher.platform.domain.ManualPublication;
import io.contentpublisher.platform.domain.Publication;
import io.contentpublisher.platform.domain.PublicationStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PublicationQueryApplicationService {
    private final ArticleRepository articles;
    private final ChannelAccountRepository accounts;
    private final PublicationRepository publications;
    private final ManualPublicationRepository manualPublications;

    public PublicationQueryApplicationService(ArticleRepository articles, ChannelAccountRepository accounts,
                                              PublicationRepository publications,
                                              ManualPublicationRepository manualPublications) {
        this.articles = articles;
        this.accounts = accounts;
        this.publications = publications;
        this.manualPublications = manualPublications;
    }

    public List<ManualPublication> getManualPublications(ActorContext actor, UUID articleId) {
        requireArticle(actor, articleId);
        return manualPublications.findByArticle(actor.tenantId(), articleId);
    }

    public List<ManualPublication> listRecentManualPublications(ActorContext actor, int limit) {
        return manualPublications.findRecent(actor.tenantId(), requireListLimit(limit));
    }

    public List<PublicationRecord> getArticlePublicationRecords(ActorContext actor, UUID articleId) {
        requireArticle(actor, articleId);
        Map<UUID, String> accountNames = accountNames(actor);
        List<PublicationRecord> records = new ArrayList<>();
        publications.findApiByArticle(actor.tenantId(), articleId).stream()
                .map(publication -> toRecord(publication, accountNames)).forEach(records::add);
        manualPublications.findByArticle(actor.tenantId(), articleId).stream()
                .map(this::toRecord).forEach(records::add);
        return sortRecords(records);
    }

    public List<PublicationRecord> listPublicationRecords(ActorContext actor, int limit) {
        int normalizedLimit = requireListLimit(limit);
        Map<UUID, String> accountNames = accountNames(actor);
        List<PublicationRecord> records = new ArrayList<>();
        publications.findRecentApi(actor.tenantId(), normalizedLimit).stream()
                .map(publication -> toRecord(publication, accountNames)).forEach(records::add);
        manualPublications.findRecent(actor.tenantId(), normalizedLimit).stream()
                .map(this::toRecord).forEach(records::add);
        return sortRecords(records).stream().limit(normalizedLimit).toList();
    }

    public List<PublicationRecord> listPublicationRecordsForArticles(ActorContext actor, List<UUID> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) return List.of();
        List<UUID> normalizedIds = articleIds.stream().distinct().toList();
        if (normalizedIds.size() > 50) throw new IllegalArgumentException("发布矩阵最多查询 50 篇文章");
        Map<UUID, String> accountNames = accountNames(actor);
        List<PublicationRecord> records = new ArrayList<>();
        publications.findApiByArticles(actor.tenantId(), normalizedIds).stream()
                .map(publication -> toRecord(publication, accountNames)).forEach(records::add);
        manualPublications.findByArticles(actor.tenantId(), normalizedIds).stream()
                .map(this::toRecord).forEach(records::add);
        return sortRecords(records);
    }

    public Publication getPublication(ActorContext actor, UUID publicationId) {
        return publications.findPublicationById(actor.tenantId(), publicationId)
                .orElseThrow(() -> new ApplicationException("PUBLICATION_NOT_FOUND", "发布记录不存在"));
    }

    private void requireArticle(ActorContext actor, UUID articleId) {
        articles.findArticleById(actor.tenantId(), articleId)
                .orElseThrow(() -> new ApplicationException("ARTICLE_NOT_FOUND", "文章不存在"));
    }

    private Map<UUID, String> accountNames(ActorContext actor) {
        Map<UUID, String> names = new HashMap<>();
        accounts.findAll(actor.tenantId()).forEach(account -> names.put(account.id(), account.displayName()));
        return names;
    }

    private PublicationRecord toRecord(Publication publication, Map<UUID, String> accountNames) {
        return new PublicationRecord(publication.id(), publication.articleId(), publication.channelType(),
                PublicationMethod.API, publication.status(), publication.channelAccountId(),
                accountNames.get(publication.channelAccountId()),
                ChannelCatalog.definition(publication.channelType()).contentFormat(), publication.externalUrl(),
                publication.errorCode(), publication.errorMessage(), null,
                publication.createdAt(), publication.updatedAt());
    }

    private PublicationRecord toRecord(ManualPublication publication) {
        return new PublicationRecord(publication.id(), publication.articleId(), publication.channelType(),
                PublicationMethod.MANUAL, PublicationStatus.PUBLISHED, null, null, publication.contentFormat(),
                publication.externalUrl(), null, null, publication.publishedBy(),
                publication.publishedAt(), publication.publishedAt());
    }

    private List<PublicationRecord> sortRecords(List<PublicationRecord> records) {
        return records.stream().sorted(Comparator.comparing(PublicationRecord::updatedAt).reversed()
                .thenComparing(PublicationRecord::id)).toList();
    }

    private int requireListLimit(int limit) {
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("列表查询数量必须在 1 到 100 之间");
        return limit;
    }
}
