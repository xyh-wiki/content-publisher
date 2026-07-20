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
            entry(ChannelType.XIAOHONGSHU, "小红书", false, ContentFormat.PLAIN_TEXT, 1_000,
                    "https://creator.xiaohongshu.com/publish/publish", "标题、正文与话题标签", List.of("xiaohongshu.com"), ChannelRegion.DOMESTIC),
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
                editorUrl, contentHint, publishedHosts, region));
    }

    public enum ChannelRegion {
        DOMESTIC,
        OVERSEAS
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
            ChannelRegion region) {
        public ChannelDefinition {
            publishedHosts = List.copyOf(publishedHosts);
        }

        public boolean manualAvailable() {
            return editorUrl != null;
        }
    }
}
