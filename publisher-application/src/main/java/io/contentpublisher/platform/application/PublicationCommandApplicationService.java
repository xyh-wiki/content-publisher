package io.contentpublisher.platform.application;

import io.contentpublisher.platform.application.port.ArticleRepository;
import io.contentpublisher.platform.application.port.AuditRecorder;
import io.contentpublisher.platform.application.port.ChannelAccountRepository;
import io.contentpublisher.platform.application.port.ChannelEndpointPolicy;
import io.contentpublisher.platform.application.port.ChannelPublisher;
import io.contentpublisher.platform.application.port.CredentialVault;
import io.contentpublisher.platform.application.port.ManualPublicationRepository;
import io.contentpublisher.platform.application.port.PublicationRepository;
import io.contentpublisher.platform.domain.AdaptedContent;
import io.contentpublisher.platform.domain.ActorContext;
import io.contentpublisher.platform.domain.Article;
import io.contentpublisher.platform.domain.ArticleStatus;
import io.contentpublisher.platform.domain.ChannelAccount;
import io.contentpublisher.platform.domain.ChannelAccountStatus;
import io.contentpublisher.platform.domain.ChannelType;
import io.contentpublisher.platform.domain.ContentFormat;
import io.contentpublisher.platform.domain.ManualPublication;
import io.contentpublisher.platform.domain.Publication;
import io.contentpublisher.platform.domain.PublicationStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class PublicationCommandApplicationService {
    private static final int MANUAL_CONTENT_MAX_CHARACTERS = 100_000;

    private final ArticleRepository articles;
    private final ChannelAccountRepository accounts;
    private final PublicationRepository publications;
    private final ManualPublicationRepository manualPublications;
    private final CredentialVault credentialVault;
    private final ChannelEndpointPolicy endpointPolicy;
    private final Map<ChannelType, ChannelPublisher> publishers;
    private final AuditRecorder auditRecorder;
    private final PlatformContentAdapter contentAdapter;
    private final Clock clock;

    public PublicationCommandApplicationService(ArticleRepository articles, ChannelAccountRepository accounts,
                                                PublicationRepository publications,
                                                ManualPublicationRepository manualPublications,
                                                CredentialVault credentialVault,
                                                ChannelEndpointPolicy endpointPolicy,
                                                List<ChannelPublisher> publishers, AuditRecorder auditRecorder,
                                                PlatformContentAdapter contentAdapter, Clock clock) {
        this.articles = articles;
        this.accounts = accounts;
        this.publications = publications;
        this.manualPublications = manualPublications;
        this.credentialVault = credentialVault;
        this.endpointPolicy = endpointPolicy;
        this.publishers = new EnumMap<>(ChannelType.class);
        publishers.forEach(publisher -> this.publishers.put(publisher.channelType(), publisher));
        this.auditRecorder = auditRecorder;
        this.contentAdapter = contentAdapter;
        this.clock = clock;
    }

    public AdaptedContent adaptContent(ActorContext actor, UUID articleId, ChannelType channelType,
                                       String canonicalUrl) {
        Article article = getArticle(actor, articleId);
        return contentAdapter.adaptForManual(article, channelType, validateCanonicalUrl(canonicalUrl));
    }

    public ManualPublication completeManualPublication(ActorContext actor, UUID articleId, ChannelType channelType,
                                                       String adaptedTitle, String adaptedContent,
                                                       ContentFormat contentFormat, String externalUrl) {
        Article article = getArticle(actor, articleId);
        if (article.status() != ArticleStatus.APPROVED && article.status() != ArticleStatus.PUBLISHED) {
            throw new ApplicationException("ARTICLE_NOT_APPROVED", "文章必须审核通过后才能记录发布结果");
        }
        ChannelCatalog.ChannelDefinition definition = ChannelCatalog.definition(channelType);
        if (!definition.manualAvailable()) {
            throw new ApplicationException("MANUAL_PUBLISH_UNAVAILABLE", "该渠道尚未配置安全的人工发布入口");
        }
        if (contentFormat != definition.contentFormat()) {
            throw new ApplicationException("CONTENT_FORMAT_INVALID", "发布内容格式与渠道要求不一致");
        }
        String title = requireText(adaptedTitle, "平台标题不能为空", 500);
        String content = requireText(adaptedContent, "平台发布内容不能为空", MANUAL_CONTENT_MAX_CHARACTERS);
        String verifiedExternalUrl = validatePublishedUrl(externalUrl, definition);
        Instant now = clock.instant();
        ManualPublication saved = manualPublications.save(new ManualPublication(UUID.randomUUID(), actor.tenantId(),
                articleId, channelType, contentFormat, title, content, verifiedExternalUrl, actor.subject(), now));
        if (article.status() != ArticleStatus.PUBLISHED) updateArticleStatus(article, ArticleStatus.PUBLISHED, actor.subject());
        auditRecorder.record(actor, "ARTICLE_MANUALLY_PUBLISHED", "MANUAL_PUBLICATION", saved.id(),
                Map.of("articleId", articleId.toString(), "channelType", channelType.name()));
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
        return publish(actor, articleId, accountId, canonicalUrl, publicationJobId, JobProgressReporter.noop());
    }

    public Publication publish(ActorContext actor, UUID articleId, UUID accountId, String canonicalUrl,
                               UUID publicationJobId, JobProgressReporter progress) {
        progress.update(20, "校验发布条件", "正在核对文章审核状态、发布账号和渠道配置");
        Publication existing = publications.findByPublicationJobId(actor.tenantId(), publicationJobId).orElse(null);
        if (existing != null) {
            if (existing.status() == PublicationStatus.PUBLISHED) {
                Article existingArticle = getArticle(actor, existing.articleId());
                if (existingArticle.status() != ArticleStatus.PUBLISHED) {
                    updateArticleStatus(existingArticle, ArticleStatus.PUBLISHED, actor.subject());
                }
                progress.update(92, "复用发布结果", "检测到平台已经返回成功结果，正在完成状态同步");
                return existing;
            }
            throw new ApplicationException("PUBLICATION_ALREADY_ATTEMPTED", "发布任务已执行，请先核对渠道结果再重放");
        }
        assertPublishable(actor, articleId, accountId);
        Article article = getArticle(actor, articleId);
        ChannelAccount account = getAccount(actor, accountId);
        ChannelPublisher publisher = publishers.get(account.type());
        if (publisher == null) throw new ApplicationException("CHANNEL_NOT_SUPPORTED", "当前渠道尚未配置发布适配器");
        String verifiedBaseUrl = endpointPolicy.validateAndNormalize(account.type(), account.baseUrl());
        if (!verifiedBaseUrl.equals(account.baseUrl())) {
            throw new ApplicationException("CHANNEL_ENDPOINT_REJECTED", "渠道地址与已保存的安全地址不一致");
        }
        progress.update(38, "创建发布记录", "发布条件校验完成，正在创建平台发布记录");
        Instant now = clock.instant();
        Publication publication = publications.save(new Publication(UUID.randomUUID(), actor.tenantId(), articleId,
                accountId, publicationJobId, account.type(), canonicalUrl, PublicationStatus.PUBLISHING, null, null,
                null, null, null, now, now));
        ChannelPublisher.PublishResult result;
        try {
            progress.update(52, "适配平台格式", "正在按照目标平台限制转换标题、正文和链接");
            AdaptedContent adaptedContent = contentAdapter.adapt(article, account.type(), canonicalUrl);
            progress.update(68, "调用平台接口", "内容适配完成，正在等待目标平台返回发布结果");
            result = publisher.publish(account,
                    new ChannelPublisher.PublishContent(article, adaptedContent, canonicalUrl),
                    credentialVault.decrypt(account.encryptedCredentials()));
        } catch (ApplicationException exception) {
            progress.update(88, "记录发布失败", "平台返回失败，正在保存错误代码和失败信息");
            Instant failedAt = clock.instant();
            publications.save(new Publication(publication.id(), publication.tenantId(), articleId, accountId,
                    publicationJobId, account.type(), canonicalUrl, PublicationStatus.FAILED, null, null,
                    exception.code(), limited(exception.getMessage()), null, publication.createdAt(), failedAt));
            throw exception;
        } catch (RuntimeException exception) {
            progress.update(88, "记录发布失败", "渠道调用发生异常，正在保存安全的失败信息");
            Instant failedAt = clock.instant();
            publications.save(new Publication(publication.id(), publication.tenantId(), articleId, accountId,
                    publicationJobId, account.type(), canonicalUrl, PublicationStatus.FAILED, null, null,
                    "CHANNEL_INTERNAL_ERROR", "渠道发布发生内部错误", null, publication.createdAt(), failedAt));
            throw new ApplicationException("CHANNEL_INTERNAL_ERROR", "渠道发布发生内部错误", exception);
        }
        progress.update(88, "保存平台结果", "平台发布成功，正在保存外部编号和结果地址");
        Instant completedAt = clock.instant();
        Publication published = publications.save(new Publication(publication.id(), publication.tenantId(),
                articleId, accountId, publicationJobId, account.type(), canonicalUrl, PublicationStatus.PUBLISHED,
                result.externalId(), result.externalUrl(), null, null, completedAt,
                publication.createdAt(), completedAt));
        updateArticleStatus(article, ArticleStatus.PUBLISHED, actor.subject());
        auditRecorder.record(actor, "ARTICLE_PUBLISHED", "PUBLICATION", published.id(),
                Map.of("articleId", articleId.toString(), "channelType", account.type().name()));
        progress.update(95, "发布结果已保存", "平台结果和文章发布状态已经完成落库");
        return published;
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

    public String validatePublishedUrl(ChannelType channelType, String value) {
        return validatePublishedUrl(value, ChannelCatalog.definition(channelType));
    }

    private String validatePublishedUrl(String value, ChannelCatalog.ChannelDefinition definition) {
        String normalized = validateCanonicalUrl(requireText(value, "发布后的文章链接不能为空", 2048));
        String host = java.net.URI.create(normalized).getHost().toLowerCase(Locale.ROOT);
        boolean allowed = definition.publishedHosts().stream()
                .anyMatch(domain -> host.equals(domain) || host.endsWith("." + domain));
        if (!allowed) {
            throw new ApplicationException("PUBLISHED_URL_REJECTED",
                    "发布结果域名 " + host + " 与所选渠道不匹配；允许域名："
                            + String.join("、", definition.publishedHosts()));
        }
        return normalized;
    }

    private Article getArticle(ActorContext actor, UUID articleId) {
        return articles.findArticleById(actor.tenantId(), articleId)
                .orElseThrow(() -> new ApplicationException("ARTICLE_NOT_FOUND", "文章不存在"));
    }

    private ChannelAccount getAccount(ActorContext actor, UUID accountId) {
        return accounts.findChannelAccountById(actor.tenantId(), accountId)
                .orElseThrow(() -> new ApplicationException("CHANNEL_ACCOUNT_NOT_FOUND", "渠道账号不存在"));
    }

    private Article updateArticleStatus(Article article, ArticleStatus status, String subject) {
        return articles.save(new Article(article.id(), article.tenantId(), article.origin(), article.generationJobId(),
                article.title(), article.summary(), article.markdown(), article.tags(), article.keywords(),
                article.titleEn(), article.summaryEn(), article.markdownEn(), article.tagsEn(), article.keywordsEn(),
                article.language(), article.sourceRevision(), article.currentVersion(), status, article.createdBy(), subject,
                article.createdAt(), clock.instant()));
    }

    private String requireText(String value, String message, int maxLength) {
        if (value == null || value.isBlank()) throw new ApplicationException("INVALID_ARGUMENT", message);
        String normalized = value.trim();
        if (normalized.length() > maxLength) throw new ApplicationException("INVALID_ARGUMENT", "字段长度超过限制");
        return normalized;
    }

    private String limited(String value) {
        if (value == null) return null;
        return value.length() <= 2000 ? value : value.substring(0, 2000);
    }
}
