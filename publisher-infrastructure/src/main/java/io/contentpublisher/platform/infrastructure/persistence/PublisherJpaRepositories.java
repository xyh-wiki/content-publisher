package io.contentpublisher.platform.infrastructure.persistence;

import io.contentpublisher.platform.domain.JobStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ProjectJpaRepository extends JpaRepository<ProjectEntity, UUID> {
    Optional<ProjectEntity> findByTenantIdAndGitUrl(String tenantId, String gitUrl);
    Optional<ProjectEntity> findByTenantIdAndId(String tenantId, UUID id);
    List<ProjectEntity> findByTenantIdOrderByUpdatedAtDesc(String tenantId, Pageable pageable);
}

interface ArticleJpaRepository extends JpaRepository<ArticleEntity, UUID> {
    Optional<ArticleEntity> findByTenantIdAndIdAndDeletedAtIsNull(String tenantId, UUID id);
    Optional<ArticleEntity> findByTenantIdAndIdAndDeletedAtIsNotNull(String tenantId, UUID id);
    Optional<ArticleEntity> findByTenantIdAndGenerationJobIdAndDeletedAtIsNull(String tenantId, UUID generationJobId);
    Optional<ArticleEntity> findByTenantIdAndGenerationJobIdAndDeletedAtIsNotNull(String tenantId, UUID generationJobId);
    List<ArticleEntity> findByTenantIdAndDeletedAtIsNullOrderByUpdatedAtDesc(String tenantId, Pageable pageable);
    List<ArticleEntity> findByTenantIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(String tenantId, Pageable pageable);
    List<ArticleEntity> findByTenantIdAndProjectIdAndDeletedAtIsNullOrderByUpdatedAtDesc(String tenantId,
                                                                                          UUID projectId,
                                                                                          Pageable pageable);
}

interface ArticleVersionJpaRepository extends JpaRepository<ArticleVersionEntity, ArticleVersionKey> {
    @Query("select v from ArticleVersionEntity v where v.tenantId = :tenantId and v.id.articleId = :articleId order by v.id.versionNumber desc")
    List<ArticleVersionEntity> findVersions(String tenantId, UUID articleId);
}

interface SnapshotJpaRepository extends JpaRepository<SnapshotEntity, UUID> {
    Optional<SnapshotEntity> findByTenantIdAndProjectId(String tenantId, UUID projectId);
}

interface AuditLogJpaRepository extends JpaRepository<AuditLogEntity, UUID> {}

interface AiProviderSettingsJpaRepository extends JpaRepository<AiProviderSettingsEntity, String> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update AiProviderSettingsEntity s set s.baseUrl = :baseUrl,
                s.encryptedApiKey = :encryptedApiKey, s.apiKeyFingerprint = :apiKeyFingerprint,
                s.model = :model, s.timeoutSeconds = :timeoutSeconds, s.temperature = :temperature,
                s.enabled = :enabled, s.settingsVersion = :nextVersion,
                s.updatedBy = :updatedBy, s.updatedAt = :updatedAt
            where s.tenantId = :tenantId and s.settingsVersion = :expectedVersion
            """)
    int updateIfVersionMatches(String tenantId, String baseUrl, String encryptedApiKey, String apiKeyFingerprint,
                               String model, int timeoutSeconds, BigDecimal temperature, boolean enabled,
                               int expectedVersion, int nextVersion, String updatedBy, Instant updatedAt);
}

interface ChannelAccountJpaRepository extends JpaRepository<ChannelAccountEntity, UUID> {
    Optional<ChannelAccountEntity> findByTenantIdAndId(String tenantId, UUID id);
    Optional<ChannelAccountEntity> findByTenantIdAndIdempotencyKey(String tenantId, String idempotencyKey);
    List<ChannelAccountEntity> findAllByTenantIdOrderByCreatedAtDesc(String tenantId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ChannelAccountEntity a set a.encryptedCredentials = :encryptedCredentials,
                a.credentialFingerprint = :credentialFingerprint, a.status = :status,
                a.accountVersion = :nextVersion, a.updatedBy = :updatedBy, a.updatedAt = :updatedAt
            where a.tenantId = :tenantId and a.id = :id and a.accountVersion = :expectedVersion
            """)
    int updateIfVersionMatches(String tenantId, UUID id, String encryptedCredentials,
                               String credentialFingerprint,
                               io.contentpublisher.platform.domain.ChannelAccountStatus status,
                               int expectedVersion, int nextVersion, String updatedBy, Instant updatedAt);
}

interface PublicationJpaRepository extends JpaRepository<PublicationEntity, UUID> {
    Optional<PublicationEntity> findByTenantIdAndIdAndDeletedAtIsNull(String tenantId, UUID id);
    Optional<PublicationEntity> findByTenantIdAndPublicationJobIdAndDeletedAtIsNull(String tenantId,
                                                                                     UUID publicationJobId);
    List<PublicationEntity> findByTenantIdAndArticleIdAndDeletedAtIsNullOrderByUpdatedAtDesc(String tenantId,
                                                                                              UUID articleId);
    List<PublicationEntity> findByTenantIdAndArticleIdInAndDeletedAtIsNullOrderByUpdatedAtDesc(String tenantId,
                                                                                                List<UUID> articleIds);
    List<PublicationEntity> findByTenantIdAndDeletedAtIsNullOrderByUpdatedAtDesc(String tenantId, Pageable pageable);
    List<PublicationEntity> findAllByTenantIdAndArticleId(String tenantId, UUID articleId);
    Optional<PublicationEntity> findByTenantIdAndPublicationJobId(String tenantId, UUID publicationJobId);
}

interface ManualPublicationJpaRepository extends JpaRepository<ManualPublicationEntity, UUID> {
    List<ManualPublicationEntity> findByTenantIdAndArticleIdAndDeletedAtIsNullOrderByPublishedAtDesc(String tenantId,
                                                                                                      UUID articleId);
    List<ManualPublicationEntity> findByTenantIdAndArticleIdInAndDeletedAtIsNullOrderByPublishedAtDesc(
            String tenantId, List<UUID> articleIds);
    List<ManualPublicationEntity> findByTenantIdAndDeletedAtIsNullOrderByPublishedAtDesc(String tenantId,
                                                                                          Pageable pageable);
    List<ManualPublicationEntity> findAllByTenantIdAndArticleId(String tenantId, UUID articleId);
}

interface JobJpaRepository extends JpaRepository<JobEntity, UUID> {
    Optional<JobEntity> findByTenantIdAndIdAndDeletedAtIsNull(String tenantId, UUID id);
    Optional<JobEntity> findByTenantIdAndIdAndDeletedAtIsNotNull(String tenantId, UUID id);
    Optional<JobEntity> findByTenantIdAndIdempotencyKey(String tenantId, String idempotencyKey);
    List<JobEntity> findByTenantIdAndDeletedAtIsNullOrderByUpdatedAtDesc(String tenantId, Pageable pageable);
    List<JobEntity> findByTenantIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(String tenantId, Pageable pageable);
    List<JobEntity> findAllByTenantIdAndDeletedAtIsNull(String tenantId);
    List<JobEntity> findAllByTenantIdAndDeletedAtIsNotNull(String tenantId);
    long countByTenantIdAndDeletedAtIsNullAndStatusIn(String tenantId, List<JobStatus> statuses);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update JobEntity j set j.status = :failedStatus, j.errorCode = :errorCode,
                j.errorMessage = :errorMessage, j.lockOwner = null, j.lockedAt = null, j.updatedAt = :now
            where j.deletedAt is null and j.status = :runningStatus
                and j.lockedAt < :staleBefore and j.attempt >= j.maxAttempts
            """)
    int failExpiredExhaustedLeases(JobStatus runningStatus, JobStatus failedStatus,
                                   Instant staleBefore, Instant now, String errorCode, String errorMessage);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select j from JobEntity j
            where j.deletedAt is null and j.attempt < j.maxAttempts and (
                (j.status in :readyStatuses and j.scheduledAt <= :now)
                or (j.status = :runningStatus and j.lockedAt < :staleBefore)
            )
            order by j.scheduledAt asc, j.createdAt asc
            """)
    List<JobEntity> findClaimable(List<JobStatus> readyStatuses, JobStatus runningStatus,
                                  Instant now, Instant staleBefore, Pageable pageable);
}
