package io.contentpublisher.platform.infrastructure.persistence;

import io.contentpublisher.platform.application.port.ChannelAccountRepository;
import io.contentpublisher.platform.application.port.ManualPublicationRepository;
import io.contentpublisher.platform.application.port.PublicationRepository;
import io.contentpublisher.platform.domain.ChannelAccount;
import io.contentpublisher.platform.domain.ManualPublication;
import io.contentpublisher.platform.domain.Publication;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional
public class JpaPublishingPersistenceAdapter implements ChannelAccountRepository, PublicationRepository,
        ManualPublicationRepository {
    private final ChannelAccountJpaRepository channelAccounts;
    private final PublicationJpaRepository publications;
    private final ManualPublicationJpaRepository manualPublications;
    private final JpaDomainMapper mapper;

    public JpaPublishingPersistenceAdapter(ChannelAccountJpaRepository channelAccounts,
                                           PublicationJpaRepository publications,
                                           ManualPublicationJpaRepository manualPublications,
                                           JpaDomainMapper mapper) {
        this.channelAccounts = channelAccounts;
        this.publications = publications;
        this.manualPublications = manualPublications;
        this.mapper = mapper;
    }

    @Override
    public ChannelAccount save(ChannelAccount account) {
        ChannelAccountEntity entity = new ChannelAccountEntity();
        entity.id = account.id(); entity.tenantId = account.tenantId(); entity.type = account.type();
        entity.displayName = account.displayName(); entity.baseUrl = account.baseUrl();
        entity.encryptedCredentials = account.encryptedCredentials(); entity.idempotencyKey = account.idempotencyKey();
        entity.requestHash = account.requestHash(); entity.credentialFingerprint = account.credentialFingerprint();
        entity.accountVersion = account.version(); entity.status = account.status();
        entity.createdBy = account.createdBy(); entity.updatedBy = account.updatedBy();
        entity.createdAt = account.createdAt(); entity.updatedAt = account.updatedAt();
        return mapper.channelAccount(channelAccounts.save(entity));
    }

    @Override
    public Optional<ChannelAccount> updateIfVersionMatches(ChannelAccount account, int expectedVersion) {
        int updated = channelAccounts.updateIfVersionMatches(account.tenantId(), account.id(),
                account.encryptedCredentials(), account.credentialFingerprint(), account.status(), expectedVersion,
                account.version(), account.updatedBy(), account.updatedAt());
        if (updated == 0) return Optional.empty();
        return channelAccounts.findByTenantIdAndId(account.tenantId(), account.id()).map(mapper::channelAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ChannelAccount> findChannelAccountById(String tenantId, UUID id) {
        return channelAccounts.findByTenantIdAndId(tenantId, id).map(mapper::channelAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ChannelAccount> findChannelAccountByIdempotencyKey(String tenantId, String idempotencyKey) {
        return channelAccounts.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey)
                .map(mapper::channelAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChannelAccount> findAll(String tenantId) {
        return channelAccounts.findAllByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(mapper::channelAccount).toList();
    }

    @Override
    public Publication save(Publication publication) {
        PublicationEntity entity = new PublicationEntity();
        entity.id = publication.id(); entity.tenantId = publication.tenantId();
        entity.articleId = publication.articleId(); entity.channelAccountId = publication.channelAccountId();
        entity.publicationJobId = publication.publicationJobId(); entity.channelType = publication.channelType();
        entity.canonicalUrl = publication.canonicalUrl(); entity.status = publication.status();
        entity.externalId = publication.externalId(); entity.externalUrl = publication.externalUrl();
        entity.errorCode = publication.errorCode(); entity.errorMessage = publication.errorMessage();
        entity.publishedAt = publication.publishedAt(); entity.createdAt = publication.createdAt();
        entity.updatedAt = publication.updatedAt();
        return mapper.publication(publications.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Publication> findPublicationById(String tenantId, UUID id) {
        return publications.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, id).map(mapper::publication);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Publication> findByPublicationJobId(String tenantId, UUID jobId) {
        return publications.findByTenantIdAndPublicationJobIdAndDeletedAtIsNull(tenantId, jobId)
                .map(mapper::publication);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Publication> findApiByArticle(String tenantId, UUID articleId) {
        return publications.findByTenantIdAndArticleIdAndDeletedAtIsNullOrderByUpdatedAtDesc(tenantId, articleId)
                .stream().map(mapper::publication).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Publication> findApiByArticles(String tenantId, List<UUID> articleIds) {
        return publications.findByTenantIdAndArticleIdInAndDeletedAtIsNullOrderByUpdatedAtDesc(tenantId, articleIds)
                .stream().map(mapper::publication).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Publication> findRecentApi(String tenantId, int limit) {
        return publications.findByTenantIdAndDeletedAtIsNullOrderByUpdatedAtDesc(
                tenantId, PageRequest.of(0, limit)).stream().map(mapper::publication).toList();
    }

    @Override
    public ManualPublication save(ManualPublication publication) {
        ManualPublicationEntity entity = new ManualPublicationEntity();
        entity.id = publication.id(); entity.tenantId = publication.tenantId();
        entity.articleId = publication.articleId(); entity.channelType = publication.channelType();
        entity.contentFormat = publication.contentFormat(); entity.adaptedTitle = publication.adaptedTitle();
        entity.adaptedContent = publication.adaptedContent(); entity.externalUrl = publication.externalUrl();
        entity.publishedBy = publication.publishedBy(); entity.publishedAt = publication.publishedAt();
        return mapper.manualPublication(manualPublications.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManualPublication> findByArticle(String tenantId, UUID articleId) {
        return manualPublications.findByTenantIdAndArticleIdAndDeletedAtIsNullOrderByPublishedAtDesc(
                tenantId, articleId).stream().map(mapper::manualPublication).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManualPublication> findByArticles(String tenantId, List<UUID> articleIds) {
        return manualPublications.findByTenantIdAndArticleIdInAndDeletedAtIsNullOrderByPublishedAtDesc(
                tenantId, articleIds).stream().map(mapper::manualPublication).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManualPublication> findRecent(String tenantId, int limit) {
        return manualPublications.findByTenantIdAndDeletedAtIsNullOrderByPublishedAtDesc(
                tenantId, PageRequest.of(0, limit)).stream().map(mapper::manualPublication).toList();
    }
}
