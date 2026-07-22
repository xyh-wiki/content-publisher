package io.contentpublisher.platform.application;

import io.contentpublisher.platform.domain.ChannelType;
import io.contentpublisher.platform.domain.ContentFormat;

import java.util.List;
import java.util.Map;

public final class ChannelCatalog {
    private static final Map<ChannelType, ChannelDefinition> DEFINITIONS = Map.ofEntries(
            entry(ChannelType.DEV, "DEV Community", true, ContentFormat.MARKDOWN, 20_000,
                    "https://dev.to/new", "Markdown 技术文章", List.of("dev.to"), ChannelRegion.OVERSEAS),
            entry(ChannelType.WORDPRESS, "WordPress", true, ContentFormat.MARKDOWN, 20_000,
                    "https://wordpress.com/posts", "Markdown 转富文本", List.of("wordpress.com"), ChannelRegion.OVERSEAS),
            entry(ChannelType.DISCOURSE, "Discourse", true, ContentFormat.MARKDOWN, 20_000,
                    null, "Markdown 论坛主题", List.of(), ChannelRegion.OVERSEAS),
            entry(ChannelType.GITHUB_DISCUSSIONS, "GitHub Discussions", true, ContentFormat.MARKDOWN, 20_000,
                    "https://github.com", "Markdown 讨论帖", List.of("github.com"), ChannelRegion.OVERSEAS),
            entry(ChannelType.X, "Twitter / X", true, ContentFormat.SHORT_TEXT, 280,
                    "https://x.com/compose/post", "280 字短帖", List.of("x.com", "twitter.com"), ChannelRegion.OVERSEAS),
            entry(ChannelType.REDDIT, "Reddit", true, ContentFormat.MARKDOWN, 10_000,
                    "https://www.reddit.com/submit", "Markdown 社区帖", List.of("reddit.com"), ChannelRegion.OVERSEAS),
            entry(ChannelType.HASHNODE, "Hashnode", true, ContentFormat.MARKDOWN, 20_000,
                    "https://hashnode.com/draft", "Markdown 技术文章", List.of("hashnode.com"), ChannelRegion.OVERSEAS),
            entry(ChannelType.MEDIUM, "Medium", true, ContentFormat.MARKDOWN, 20_000,
                    "https://medium.com/new-story", "Markdown 长文章", List.of("medium.com"), ChannelRegion.OVERSEAS),
            entry(ChannelType.MASTODON, "Mastodon", true, ContentFormat.SHORT_TEXT, 500,
                    null, "500 字短帖", List.of(), ChannelRegion.OVERSEAS),
            entry(ChannelType.GHOST, "Ghost", true, ContentFormat.MARKDOWN, 20_000,
                    null, "Markdown 转富文本", List.of(), ChannelRegion.OVERSEAS),
            entry(ChannelType.XIAOHONGSHU, "小红书 / RedNote", false, ContentFormat.PLAIN_TEXT, 1_000,
                    "https://creator.xiaohongshu.com/publish/publish", "标题、正文与话题标签",
                    List.of("xiaohongshu.com", "rednote.com", "xhslink.com"), ChannelRegion.DOMESTIC),
            entry(ChannelType.CSDN, "CSDN", false, ContentFormat.MARKDOWN, 20_000,
                    "https://editor.csdn.net/md/", "Markdown 技术文章", List.of("csdn.net"), ChannelRegion.DOMESTIC),
            entry(ChannelType.JUEJIN, "掘金", false, ContentFormat.MARKDOWN, 20_000,
                    "https://juejin.cn/editor/drafts/new", "Markdown 技术文章", List.of("juejin.cn"), ChannelRegion.DOMESTIC),
            entry(ChannelType.ZHIHU, "知乎", false, ContentFormat.PLAIN_TEXT, 20_000,
                    "https://zhuanlan.zhihu.com/write", "纯文本长文章", List.of("zhihu.com"), ChannelRegion.DOMESTIC),
            entry(ChannelType.CNBLOGS, "博客园", false, ContentFormat.MARKDOWN, 20_000,
                    "https://i.cnblogs.com/posts/edit", "Markdown 技术文章", List.of("cnblogs.com"), ChannelRegion.DOMESTIC),
            entry(ChannelType.SEGMENTFAULT, "SegmentFault", false, ContentFormat.MARKDOWN, 20_000,
                    "https://segmentfault.com/write", "Markdown 技术文章", List.of("segmentfault.com"), ChannelRegion.DOMESTIC),
            entry(ChannelType.V2EX, "V2EX", false, ContentFormat.MARKDOWN, 10_000,
                    "https://www.v2ex.com/new", "简洁 Markdown 主题", List.of("v2ex.com"), ChannelRegion.DOMESTIC),
            entry(ChannelType.OSCHINA, "开源中国", false, ContentFormat.MARKDOWN, 20_000,
                    "https://my.oschina.net/u/blog/write", "Markdown 技术文章", List.of("oschina.net"), ChannelRegion.DOMESTIC),
            entry(ChannelType.LINKEDIN, "LinkedIn", false, ContentFormat.PLAIN_TEXT, 3_000,
                    "https://www.linkedin.com/feed/?shareActive=true", "专业动态与知识短文", List.of("linkedin.com"), ChannelRegion.OVERSEAS),
            entry(ChannelType.WECHAT_OFFICIAL, "微信公众号", false, ContentFormat.PLAIN_TEXT, 20_000,
                    "https://mp.weixin.qq.com", "标题与富文本文章素材", List.of("weixin.qq.com", "qq.com"), ChannelRegion.DOMESTIC),
            entry(ChannelType.JIANSHU, "简书", false, ContentFormat.PLAIN_TEXT, 20_000,
                    "https://www.jianshu.com/writer", "知识文章与教程", List.of("jianshu.com"), ChannelRegion.DOMESTIC),
            entry(ChannelType.TOUTIAO, "今日头条", false, ContentFormat.PLAIN_TEXT, 20_000,
                    "https://mp.toutiao.com/profile_v4/graphic/publish", "图文文章素材", List.of("toutiao.com"), ChannelRegion.DOMESTIC),
            entry(ChannelType.BILIBILI_COLUMN, "B站专栏", false, ContentFormat.PLAIN_TEXT, 20_000,
                    "https://member.bilibili.com/platform/upload/text/apply", "专栏文章素材", List.of("bilibili.com"), ChannelRegion.DOMESTIC),
            entry(ChannelType.BLOG_51CTO, "51CTO 博客", false, ContentFormat.MARKDOWN, 20_000,
                    "https://blog.51cto.com/blogger/publish", "Markdown 技术博客", List.of("51cto.com"), ChannelRegion.DOMESTIC),
            entry(ChannelType.TENCENT_CLOUD, "腾讯云开发者社区", false, ContentFormat.MARKDOWN, 20_000,
                    "https://cloud.tencent.com/developer/article/write", "Markdown 技术文章", List.of("cloud.tencent.com"), ChannelRegion.DOMESTIC),
            entry(ChannelType.ALIBABA_CLOUD, "阿里云开发者社区", false, ContentFormat.MARKDOWN, 20_000,
                    "https://developer.aliyun.com/article/new", "Markdown 技术文章", List.of("developer.aliyun.com"), ChannelRegion.DOMESTIC),
            entry(ChannelType.HUAWEI_CLOUD, "华为云开发者社区", false, ContentFormat.MARKDOWN, 20_000,
                    "https://bbs.huaweicloud.com/blogs/new", "Markdown 技术文章", List.of("huaweicloud.com"), ChannelRegion.DOMESTIC)
    );

    private ChannelCatalog() {
    }

    public static ChannelDefinition definition(ChannelType type) {
        ChannelDefinition definition = DEFINITIONS.get(type);
        if (definition == null) {
            throw new IllegalArgumentException("未知发布渠道: " + type);
        }
        return definition;
    }

    public static List<ChannelDefinition> all() {
        return List.of(ChannelType.values()).stream().map(ChannelCatalog::definition).toList();
    }

    public static List<ChannelDefinition> automated() {
        return all().stream().filter(ChannelDefinition::apiSupported).toList();
    }

    public static List<ChannelDefinition> manualOnly() {
        return all().stream().filter(definition -> !definition.apiSupported()).toList();
    }

    private static Map.Entry<ChannelType, ChannelDefinition> entry(ChannelType type, String displayName,
                                                                   boolean apiSupported, ContentFormat format,
                                                                   int characterLimit, String editorUrl,
                                                                   String contentHint, List<String> publishedHosts,
                                                                   ChannelRegion region) {
        return Map.entry(type, new ChannelDefinition(type, displayName, apiSupported, format, characterLimit,
                editorUrl, contentHint, publishedHosts, region, credentialFields(type)));
    }

    private static List<CredentialField> credentialFields(ChannelType type) {
        return switch (type) {
            case DEV -> List.of(credential("apiKey", "API Key"));
            case WORDPRESS -> List.of(credential("username", "用户名"),
                    credential("applicationPassword", "Application Password"));
            case DISCOURSE -> List.of(credential("apiKey", "API Key"),
                    credential("apiUsername", "API Username"));
            case GITHUB_DISCUSSIONS -> List.of(credential("token", "Access Token"),
                    credential("repositoryId", "Repository ID"), credential("categoryId", "Category ID"));
            case X -> List.of(credential("accessToken", "Access Token"),
                    credential("refreshToken", "Refresh Token"), credential("clientId", "Client ID"),
                    credential("clientSecret", "Client Secret"));
            case REDDIT -> List.of(credential("accessToken", "Access Token"),
                    credential("refreshToken", "Refresh Token"), credential("clientId", "Client ID"),
                    credential("clientSecret", "Client Secret"), credential("subreddit", "Subreddit"));
            case HASHNODE -> List.of(credential("token", "Access Token"),
                    credential("publicationId", "Publication ID"));
            case MEDIUM -> List.of(credential("token", "Integration Token"), credential("authorId", "Author ID"));
            case MASTODON -> List.of(credential("accessToken", "Access Token"));
            case GHOST -> List.of(credential("adminApiKey", "Admin API Key"));
            case XIAOHONGSHU, CSDN, JUEJIN, ZHIHU, CNBLOGS, SEGMENTFAULT, V2EX, OSCHINA,
                    LINKEDIN, WECHAT_OFFICIAL, JIANSHU, TOUTIAO, BILIBILI_COLUMN, BLOG_51CTO,
                    TENCENT_CLOUD, ALIBABA_CLOUD, HUAWEI_CLOUD -> List.of();
        };
    }

    private static CredentialField credential(String key, String label) {
        return new CredentialField(key, label);
    }

    public enum ChannelRegion {
        DOMESTIC,
        OVERSEAS
    }

    public enum AutomationAvailability {
        AVAILABLE,
        LIMITED,
        REQUIRES_UPGRADE,
        RETIRED
    }

    public record ChannelDefinition(
            ChannelType type,
            String displayName,
            boolean apiSupported,
            ContentFormat contentFormat,
            int characterLimit,
            String editorUrl,
            String contentHint,
            List<String> publishedHosts,
            ChannelRegion region,
            List<CredentialField> credentialFields) {
        public ChannelDefinition {
            publishedHosts = List.copyOf(publishedHosts);
            credentialFields = List.copyOf(credentialFields);
        }

        public boolean manualAvailable() {
            return editorUrl != null;
        }

        public List<String> credentialKeys() {
            return credentialFields.stream().map(CredentialField::key).toList();
        }

        public String credentialLabelsAttribute() {
            return String.join("|", credentialFields.stream().map(CredentialField::label).toList());
        }

        public AutomationAvailability automationAvailability() {
            return switch (type) {
                case MEDIUM -> AutomationAvailability.RETIRED;
                default -> apiSupported ? AutomationAvailability.AVAILABLE : AutomationAvailability.RETIRED;
            };
        }

        public boolean configurationAllowed() {
            return automationAvailability() == AutomationAvailability.AVAILABLE
                    || automationAvailability() == AutomationAvailability.LIMITED;
        }

        public boolean existingAccountOperational() {
            return apiSupported && automationAvailability() != AutomationAvailability.REQUIRES_UPGRADE;
        }

        public String availabilityLabel() {
            return switch (automationAvailability()) {
                case AVAILABLE -> "可用";
                case LIMITED -> "授权受限";
                case REQUIRES_UPGRADE -> "等待系统升级";
                case RETIRED -> "停止新接入";
            };
        }

        public String availabilityTone() {
            return switch (automationAvailability()) {
                case AVAILABLE -> "success";
                case LIMITED -> "warning";
                case REQUIRES_UPGRADE -> "danger";
                case RETIRED -> "muted";
            };
        }

        public String availabilityNote() {
            return switch (type) {
                case X -> "使用 OAuth 2.0 Refresh Token，在发布前自动刷新并加密保存轮换后的令牌。";
                case REDDIT -> "需要先获批 Data API；系统会在发布前刷新用户令牌并保存轮换结果。";
                case MEDIUM -> "Medium 官方已停止支持新的 API 集成。";
                default -> apiSupported ? "可以配置并用于 API 自动发布。" : "通过官方创作页完成人工发布。";
            };
        }

        public String applicationGuideUrl() {
            return switch (type) {
                case DEV -> "https://developers.forem.com/api/v1#tag/articles/operation/createArticle";
                case WORDPRESS -> "https://developer.wordpress.org/rest-api/using-the-rest-api/authentication/#basic-authentication-with-application-passwords";
                case DISCOURSE -> "https://meta.discourse.org/t/create-and-configure-an-api-key/230124";
                case GITHUB_DISCUSSIONS -> "https://docs.github.com/en/graphql/guides/using-the-graphql-api-for-discussions";
                case X -> "https://docs.x.com/fundamentals/authentication/oauth-2-0/authorization-code";
                case REDDIT -> "https://support.reddithelp.com/hc/en-us/articles/14945211791892-Developer-Platform-Accessing-Reddit-Data";
                case HASHNODE -> "https://github.com/Hashnode/gql-skill";
                case MEDIUM -> "https://github.com/Medium/medium-api-docs";
                case MASTODON -> "https://docs.joinmastodon.org/client/token/";
                case GHOST -> "https://docs.ghost.org/admin-api/#token-authentication";
                default -> editorUrl;
            };
        }
    }

    public record CredentialField(String key, String label) {
    }
}
