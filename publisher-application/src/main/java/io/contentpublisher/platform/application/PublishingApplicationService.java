package io.contentpublisher.platform.application;

import io.contentpublisher.platform.application.port.ArticleRepository;
import io.contentpublisher.platform.application.port.AuditRecorder;
import io.contentpublisher.platform.application.port.ChannelAccountRepository;
import io.contentpublisher.platform.application.port.ChannelEndpointPolicy;
import io.contentpublisher.platform.application.port.ChannelPublisher;
import io.contentpublisher.platform.application.port.CredentialVault;
import io.contentpublisher.platform.application.port.PublicationRepository;
import io.contentpublisher.platform.domain.ActorContext;
import io.contentpublisher.platform.domain.Article;
import io.contentpublisher.platform.domain.ArticleStatus;
import io.contentpublisher.platform.domain.ArticleVersion;
import io.contentpublisher.platform.domain.ChannelAccount;
import io.contentpublisher.platform.domain.ChannelAccountStatus;
import io.contentpublisher.platform.domain.ChannelType;
import io.contentpublisher.platform.domain.Publication;
import io.contentpublisher.platform.domain.PublicationStatus;

import java.time.Clock;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PublishingApplicationService {
    private final ArticleRepository articles;
    private final ChannelAccountRepository accounts;
    private final PublicationRepository publications;
    private final CredentialVault credentialVault;
    private final ChannelEndpointPolicy endpointPolicy;
    private final Map<ChannelType, ChannelPublisher> publishers;
    private final AuditRecorder auditRecorder;
    private final Clock clock;

    public PublishingApplicationService(ArticleRepository articles,
                                        ChannelAccountRepository accounts,
                                        PublicationRepository publications,
                                        CredentialVault credentialVault,
                                        ChannelEndpointPolicy endpointPolicy,
                                        List<ChannelPublisher> publishers,
                                        AuditRecorder auditRecorder,
                                        Clock clock) {
        this.articles = articles;
        this.accounts = accounts;
        this.publications = publications;
        this.credentialVault = credentialVault;
        this.endpointPolicy = endpointPolicy;
        this.publishers = new EnumMap<>(ChannelType.class);
        publishers.forEach(publisher -> this.publishers.put(publisher.channelType(), publisher));
        this.auditRecorder = auditRecorder;
        this.clock = clock;
    }

    public ChannelAccount createAccount(ActorContext actor, ChannelType type, String displayName,
                                        String baseUrl, Map<String, String> credentials, String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);
        String name = requireText(displayName, "渠道账号名称不能为空", 120);
        Map<String, String> validatedCredentials = validateCredentials(type, credentials);
        String normalizedBaseUrl = endpointPolicy.validateAndNormalize(type, baseUrl);
        String credentialFingerprint = credentialVault.fingerprint(validatedCredentials);
        String requestHash = accountHash(type, name, normalizedBaseUrl, credentialFingerprint);
        ChannelAccount existing = accounts.findChannelAccountByIdempotencyKey(actor.tenantId(), idempotencyKey).orElse(null);
        if (existing != null) {
            if (!existing.requestHash().equals(requestHash)) {
                throw new ApplicationException("IDEMPOTENCY_KEY_CONFLICT", "幂等键已用于不同请求");
            }
            return existing;
        }
        Instant now = clock.instant();
        ChannelAccount saved = accounts.save(new ChannelAccount(UUID.randomUUID(), actor.tenantId(), type, name,
                normalizedBaseUrl, credentialVault.encrypt(validatedCredentials), idempotencyKey, requestHash,
                credentialFingerprint, 1, ChannelAccountStatus.ACTIVE,
                actor.subject(), actor.subject(), now, now));
        auditRecorder.record(actor, "CHANNEL_ACCOUNT_CREATED", "CHANNEL_ACCOUNT", saved.id(),
                Map.of("channelType", type.name()));
        return saved;
    }

    public List<ChannelAccount> listAccounts(ActorContext actor) {
        return accounts.findAll(actor.tenantId());
    }

    public ChannelAccount getAccount(ActorContext actor, UUID accountId) {
        return accounts.findChannelAccountById(actor.tenantId(), accountId)
                .orElseThrow(() -> new ApplicationException("CHANNEL_ACCOUNT_NOT_FOUND", "渠道账号不存在"));
    }

    public ChannelAccount updateAccountStatus(ActorContext actor, UUID accountId, int expectedVersion,
                                              ChannelAccountStatus status) {
        ChannelAccount account = getAccount(actor, accountId);
        if (account.status() == status && account.version() == expectedVersion + 1) return account;
        requireAccountVersion(account, expectedVersion);
        if (account.status() == status) return account;
        Instant now = clock.instant();
        ChannelAccount candidate = new ChannelAccount(account.id(), account.tenantId(), account.type(),
                account.displayName(), account.baseUrl(), account.encryptedCredentials(), account.idempotencyKey(),
                account.requestHash(), account.credentialFingerprint(), account.version() + 1, status,
                account.createdBy(), actor.subject(), account.createdAt(), now);
        ChannelAccount saved = accounts.updateIfVersionMatches(candidate, expectedVersion)
                .orElseThrow(this::accountVersionConflict);
        auditRecorder.record(actor, "CHANNEL_ACCOUNT_STATUS_CHANGED", "CHANNEL_ACCOUNT", accountId,
                Map.of("status", status.name(), "version", Integer.toString(saved.version())));
        return saved;
    }

    public ChannelAccount rotateCredentials(ActorContext actor, UUID accountId, int expectedVersion,
                                            Map<String, String> credentials) {
        ChannelAccount account = getAccount(actor, accountId);
        Map<String, String> validated = validateCredentials(account.type(), credentials);
        String fingerprint = credentialVault.fingerprint(validated);
        if (account.version() == expectedVersion + 1 && fingerprint.equals(account.credentialFingerprint())) {
            return account;
        }
        requireAccountVersion(account, expectedVersion);
        Instant now = clock.instant();
        ChannelAccount candidate = new ChannelAccount(account.id(), account.tenantId(), account.type(),
                account.displayName(), account.baseUrl(), credentialVault.encrypt(validated), account.idempotencyKey(),
                account.requestHash(), fingerprint, account.version() + 1, account.status(),
                account.createdBy(), actor.subject(), account.createdAt(), now);
        ChannelAccount saved = accounts.updateIfVersionMatches(candidate, expectedVersion)
                .orElseThrow(this::accountVersionConflict);
        auditRecorder.record(actor, "CHANNEL_CREDENTIALS_ROTATED", "CHANNEL_ACCOUNT", accountId,
                Map.of("channelType", account.type().name(), "version", Integer.toString(saved.version())));
        return saved;
    }

    public Article getArticle(ActorContext actor, UUID articleId) {
        return articles.findArticleById(actor.tenantId(), articleId)
                .orElseThrow(() -> new ApplicationException("ARTICLE_NOT_FOUND", "文章不存在"));
    }

    public List<ArticleVersion> getArticleVersions(ActorContext actor, UUID articleId) {
        getArticle(actor, articleId);
        return articles.findVersions(actor.tenantId(), articleId);
    }

    public Article updateArticle(ActorContext actor, UUID articleId, int expectedVersion, String title,
                                 String summary, String markdown, List<String> keywords) {
        Article article = getArticle(actor, articleId);
        if (article.status() == ArticleStatus.APPROVED || article.status() == ArticleStatus.PUBLISHED) {
            throw new ApplicationException("ARTICLE_STATE_CONFLICT", "审核通过或已发布文章不能直接修改");
        }
        if (article.currentVersion() != expectedVersion) {
            throw new ApplicationException("ARTICLE_VERSION_CONFLICT", "文章已被其他请求修改，请刷新后重试");
        }
        String normalizedTitle = requireText(title, "文章标题不能为空", 500);
        String normalizedSummary = requireText(summary, "文章摘要不能为空", 2000);
        String normalizedMarkdown = requireText(markdown, "文章正文不能为空", 20000);
        List<String> normalizedKeywords = normalizeKeywords(keywords);
        int nextVersion = expectedVersion + 1;
        Instant now = clock.instant();
        Article updated = new Article(article.id(), article.tenantId(), article.projectId(), article.generationJobId(),
                normalizedTitle, normalizedSummary, normalizedMarkdown, normalizedKeywords, article.language(),
                article.sourceRevision(), nextVersion, ArticleStatus.DRAFT, article.createdBy(), actor.subject(),
                article.createdAt(), now);
        Article saved = articles.saveWithVersion(updated, new ArticleVersion(actor.tenantId(), articleId, nextVersion,
                normalizedTitle, normalizedSummary, normalizedMarkdown, normalizedKeywords, actor.subject(), now));
        auditRecorder.record(actor, "ARTICLE_VERSION_CREATED", "ARTICLE", articleId,
                Map.of("version", Integer.toString(nextVersion)));
        return saved;
    }

    public Article approveArticle(ActorContext actor, UUID articleId) {
        Article article = getArticle(actor, articleId);
        if (article.status() == ArticleStatus.APPROVED || article.status() == ArticleStatus.PUBLISHED) return article;
        if (article.status() != ArticleStatus.DRAFT && article.status() != ArticleStatus.REJECTED) {
            throw new ApplicationException("ARTICLE_STATE_CONFLICT", "只有草稿或已驳回文章可以审核通过");
        }
        Article saved = updateArticleStatus(article, ArticleStatus.APPROVED, actor.subject());
        auditRecorder.record(actor, "ARTICLE_APPROVED", "ARTICLE", articleId, Map.of());
        return saved;
    }

    public Article rejectArticle(ActorContext actor, UUID articleId, String reason) {
        Article article = getArticle(actor, articleId);
        if (article.status() == ArticleStatus.REJECTED) return article;
        if (article.status() == ArticleStatus.PUBLISHED) {
            throw new ApplicationException("ARTICLE_STATE_CONFLICT", "已发布文章不能驳回");
        }
        String normalizedReason = requireText(reason, "驳回原因不能为空", 500);
        Article saved = updateArticleStatus(article, ArticleStatus.REJECTED, actor.subject());
        auditRecorder.record(actor, "ARTICLE_REJECTED", "ARTICLE", articleId,
                Map.of("reason", normalizedReason));
        return saved;
    }

    public void assertPublishable(ActorContext actor, UUID articleId, UUID accountId) {
        Article article = getArticle(actor, articleId);
        if (article.status() != ArticleStatus.APPROVED && article.status() != ArticleStatus.PUBLISHED) {
            throw new ApplicationException("ARTICLE_NOT_APPROVED", "文章必须审核通过后才能发布");
        }
        ChannelAccount account = getAccount(actor, accountId);
        if (account.status() != ChannelAccountStatus.ACTIVE) {
            throw new ApplicationException("CHANNEL_ACCOUNT_DISABLED", "渠道账号已停用");
        }
    }

    public Publication publish(ActorContext actor, UUID articleId, UUID accountId, String canonicalUrl,
                               UUID publicationJobId) {
        Publication existing = publications.findByPublicationJobId(actor.tenantId(), publicationJobId).orElse(null);
        if (existing != null) {
            if (existing.status() == PublicationStatus.PUBLISHED) {
                Article existingArticle = getArticle(actor, existing.articleId());
                if (existingArticle.status() != ArticleStatus.PUBLISHED) {
                    updateArticleStatus(existingArticle, ArticleStatus.PUBLISHED, actor.subject());
                }
                return existing;
            }
            throw new ApplicationException("PUBLICATION_ALREADY_ATTEMPTED", "发布任务已执行，请先核对渠道结果再重放");
        }
        assertPublishable(actor, articleId, accountId);
        Article article = getArticle(actor, articleId);
        ChannelAccount account = getAccount(actor, accountId);
        ChannelPublisher publisher = publishers.get(account.type());
        if (publisher == null) {
            throw new ApplicationException("CHANNEL_NOT_SUPPORTED", "当前渠道尚未配置发布适配器");
        }
        String verifiedBaseUrl = endpointPolicy.validateAndNormalize(account.type(), account.baseUrl());
        if (!verifiedBaseUrl.equals(account.baseUrl())) {
            throw new ApplicationException("CHANNEL_ENDPOINT_REJECTED", "渠道地址与已保存的安全地址不一致");
        }
        Instant now = clock.instant();
        Publication publication = publications.save(new Publication(UUID.randomUUID(), actor.tenantId(), articleId,
                accountId, publicationJobId, account.type(), canonicalUrl, PublicationStatus.PUBLISHING, null, null,
                null, null, null, now, now));
        ChannelPublisher.PublishResult result;
        try {
            result = publisher.publish(account, new ChannelPublisher.PublishContent(article, canonicalUrl),
                    credentialVault.decrypt(account.encryptedCredentials()));
        } catch (ApplicationException exception) {
            Instant failedAt = clock.instant();
            publications.save(new Publication(publication.id(), publication.tenantId(), articleId, accountId,
                    publicationJobId, account.type(), canonicalUrl, PublicationStatus.FAILED, null, null,
                    exception.code(), limited(exception.getMessage()), null, publication.createdAt(), failedAt));
            throw exception;
        } catch (RuntimeException exception) {
            Instant failedAt = clock.instant();
            publications.save(new Publication(publication.id(), publication.tenantId(), articleId, accountId,
                    publicationJobId, account.type(), canonicalUrl, PublicationStatus.FAILED, null, null,
                    "CHANNEL_INTERNAL_ERROR", "渠道发布发生内部错误", null, publication.createdAt(), failedAt));
            throw new ApplicationException("CHANNEL_INTERNAL_ERROR", "渠道发布发生内部错误", exception);
        }
        Instant completedAt = clock.instant();
        Publication published = publications.save(new Publication(publication.id(), publication.tenantId(),
                articleId, accountId, publicationJobId, account.type(), canonicalUrl, PublicationStatus.PUBLISHED,
                result.externalId(), result.externalUrl(), null, null, completedAt,
                publication.createdAt(), completedAt));
        updateArticleStatus(article, ArticleStatus.PUBLISHED, actor.subject());
        auditRecorder.record(actor, "ARTICLE_PUBLISHED", "PUBLICATION", published.id(),
                Map.of("articleId", articleId.toString(), "channelType", account.type().name()));
        return published;
    }

    public Publication getPublication(ActorContext actor, UUID publicationId) {
        return publications.findPublicationById(actor.tenantId(), publicationId)
                .orElseThrow(() -> new ApplicationException("PUBLICATION_NOT_FOUND", "发布记录不存在"));
    }

    public String validateCanonicalUrl(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            String normalized = value.trim();
            if (normalized.length() > 2048) {
                throw new ApplicationException("CANONICAL_URL_REJECTED", "外链 URL 长度不能超过 2048 字符");
            }
            java.net.URI uri = java.net.URI.create(normalized);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) {
                throw new ApplicationException("CANONICAL_URL_REJECTED", "外链必须是不含凭据的 HTTPS URL");
            }
            return normalized;
        } catch (ApplicationException exception) {
            throw exception;
        } catch (IllegalArgumentException exception) {
            throw new ApplicationException("CANONICAL_URL_REJECTED", "外链 URL 格式无效", exception);
        }
    }

    private Article updateArticleStatus(Article article, ArticleStatus status, String subject) {
        return articles.save(new Article(article.id(), article.tenantId(), article.projectId(), article.generationJobId(),
                article.title(), article.summary(), article.markdown(), article.keywords(), article.language(),
                article.sourceRevision(), article.currentVersion(), status, article.createdBy(), subject,
                article.createdAt(), clock.instant()));
    }

    private Map<String, String> validateCredentials(ChannelType type, Map<String, String> credentials) {
        Map<String, String> source = credentials == null ? Map.of() : credentials;
        List<String> required = switch (type) {
            case DEV -> List.of("apiKey");
            case WORDPRESS -> List.of("username", "applicationPassword");
            case DISCOURSE -> List.of("apiKey", "apiUsername");
            case GITHUB_DISCUSSIONS -> List.of("token", "repositoryId", "categoryId");
            case X -> List.of("accessToken");
            case REDDIT -> List.of("accessToken", "subreddit");
            case HASHNODE -> List.of("token", "publicationId");
            case MEDIUM -> List.of("token", "authorId");
            case MASTODON -> List.of("accessToken");
            case GHOST -> List.of("adminApiKey");
        };
        if (!source.keySet().equals(new java.util.HashSet<>(required))) {
            throw new ApplicationException("CHANNEL_CREDENTIALS_INVALID",
                    "渠道凭据字段必须且只能包含: " + String.join(", ", required));
        }
        java.util.LinkedHashMap<String, String> normalized = new java.util.LinkedHashMap<>();
        required.forEach(key -> normalized.put(key, requireText(source.get(key), "渠道凭据不能为空", 4000)));
        validateCredentialValues(type, normalized);
        return Map.copyOf(normalized);
    }

    private void validateCredentialValues(ChannelType type, Map<String, String> credentials) {
        if (type == ChannelType.REDDIT && !credentials.get("subreddit").matches("[A-Za-z0-9_]{3,21}")) {
            throw new ApplicationException("CHANNEL_CREDENTIALS_INVALID", "Reddit subreddit 格式无效");
        }
        if (type == ChannelType.MEDIUM && !credentials.get("authorId").matches("[A-Za-z0-9_-]{1,200}")) {
            throw new ApplicationException("CHANNEL_CREDENTIALS_INVALID", "Medium authorId 格式无效");
        }
        if (type == ChannelType.GHOST) {
            String[] parts = credentials.get("adminApiKey").split(":", 2);
            if (parts.length != 2 || parts[0].isBlank() || parts[0].length() > 200
                    || !parts[1].matches("(?:[0-9a-fA-F]{2}){16,}")) {
                throw new ApplicationException("CHANNEL_CREDENTIALS_INVALID",
                        "Ghost Admin API Key 必须为 id:hexSecret 格式");
            }
        }
    }

    private String requireText(String value, String message, int maxLength) {
        if (value == null || value.isBlank()) throw new ApplicationException("INVALID_ARGUMENT", message);
        String normalized = value.trim();
        if (normalized.length() > maxLength) throw new ApplicationException("INVALID_ARGUMENT", "字段长度超过限制");
        return normalized;
    }

    private List<String> normalizeKeywords(List<String> keywords) {
        if (keywords == null) return List.of();
        List<String> normalized = keywords.stream().filter(java.util.Objects::nonNull).map(String::trim)
                .filter(value -> !value.isEmpty()).distinct().toList();
        if (normalized.size() > 30 || normalized.stream().anyMatch(value -> value.length() > 100)) {
            throw new ApplicationException("INVALID_ARGUMENT", "关键词最多 30 个且每个不超过 100 字符");
        }
        return normalized;
    }

    private String limited(String value) {
        if (value == null) return null;
        return value.length() <= 2000 ? value : value.substring(0, 2000);
    }

    private void validateIdempotencyKey(String key) {
        if (key == null || !key.matches("[A-Za-z0-9._:-]{8,128}")) {
            throw new ApplicationException("IDEMPOTENCY_KEY_INVALID",
                    "Idempotency-Key 必须为 8 到 128 位字母、数字、点、下划线、冒号或连字符");
        }
    }

    private String accountHash(ChannelType type, String name, String baseUrl, String credentialFingerprint) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            updateDigest(digest, type.name()); updateDigest(digest, name); updateDigest(digest, baseUrl);
            updateDigest(digest, credentialFingerprint);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("运行环境缺少 SHA-256", exception);
        }
    }

    private void updateDigest(MessageDigest digest, String value) {
        digest.update(value.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0x1f);
    }

    private void requireAccountVersion(ChannelAccount account, int expectedVersion) {
        if (expectedVersion < 1 || account.version() != expectedVersion) {
            throw accountVersionConflict();
        }
    }

    private ApplicationException accountVersionConflict() {
        return new ApplicationException("CHANNEL_ACCOUNT_VERSION_CONFLICT",
                "渠道账号已被其他请求修改，请刷新后重试");
    }
}
