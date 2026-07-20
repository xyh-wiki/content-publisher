package io.contentpublisher.platform.infrastructure.persistence;

import io.contentpublisher.platform.domain.JobStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ProjectJpaRepository extends JpaRepository<ProjectEntity, UUID> {
    Optional<ProjectEntity> findByTenantIdAndGitUrl(String tenantId, String gitUrl);
    Optional<ProjectEntity> findByTenantIdAndId(String tenantId, UUID id);
}

interface ArticleJpaRepository extends JpaRepository<ArticleEntity, UUID> {
    Optional<ArticleEntity> findByTenantIdAndId(String tenantId, UUID id);
    Optional<ArticleEntity> findByTenantIdAndGenerationJobId(String tenantId, UUID generationJobId);
}

interface ArticleVersionJpaRepository extends JpaRepository<ArticleVersionEntity, ArticleVersionKey> {
    @Query("select v from ArticleVersionEntity v where v.tenantId = :tenantId and v.id.articleId = :articleId order by v.id.versionNumber desc")
    List<ArticleVersionEntity> findVersions(String tenantId, UUID articleId);
}

interface SnapshotJpaRepository extends JpaRepository<SnapshotEntity, UUID> {
    Optional<SnapshotEntity> findByTenantIdAndProjectId(String tenantId, UUID projectId);
}

interface AuditLogJpaRepository extends JpaRepository<AuditLogEntity, UUID> {}

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
    Optional<PublicationEntity> findByTenantIdAndId(String tenantId, UUID id);
    Optional<PublicationEntity> findByTenantIdAndPublicationJobId(String tenantId, UUID publicationJobId);
}

interface JobJpaRepository extends JpaRepository<JobEntity, UUID> {
    Optional<JobEntity> findByTenantIdAndId(String tenantId, UUID id);
    Optional<JobEntity> findByTenantIdAndIdempotencyKey(String tenantId, String idempotencyKey);
    long countByTenantIdAndStatusIn(String tenantId, List<JobStatus> statuses);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update JobEntity j set j.status = :failedStatus, j.errorCode = :errorCode,
                j.errorMessage = :errorMessage, j.lockOwner = null, j.lockedAt = null, j.updatedAt = :now
            where j.status = :runningStatus and j.lockedAt < :staleBefore and j.attempt >= j.maxAttempts
            """)
    int failExpiredExhaustedLeases(JobStatus runningStatus, JobStatus failedStatus,
                                   Instant staleBefore, Instant now, String errorCode, String errorMessage);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select j from JobEntity j
            where j.attempt < j.maxAttempts and (
                (j.status in :readyStatuses and j.scheduledAt <= :now)
                or (j.status = :runningStatus and j.lockedAt < :staleBefore)
            )
            order by j.scheduledAt asc, j.createdAt asc
            """)
    List<JobEntity> findClaimable(List<JobStatus> readyStatuses, JobStatus runningStatus,
                                  Instant now, Instant staleBefore, Pageable pageable);
}
