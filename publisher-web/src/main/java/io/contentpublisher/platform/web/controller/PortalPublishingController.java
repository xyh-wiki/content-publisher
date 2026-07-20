package io.contentpublisher.platform.web.controller;

import io.contentpublisher.platform.application.ApplicationException;
import io.contentpublisher.platform.application.ChannelCatalog;
import io.contentpublisher.platform.application.JobApplicationService;
import io.contentpublisher.platform.application.ProjectApplicationService;
import io.contentpublisher.platform.application.PublishingApplicationService;
import io.contentpublisher.platform.application.PublicationRecord;
import io.contentpublisher.platform.domain.ChannelType;
import io.contentpublisher.platform.domain.PublicationStatus;
import io.contentpublisher.platform.web.form.CreateChannelAccountForm;
import io.contentpublisher.platform.web.form.BatchPublishArticleForm;
import io.contentpublisher.platform.web.form.ManualPublicationForm;
import io.contentpublisher.platform.web.form.PublishArticleForm;
import io.contentpublisher.platform.web.dto.ChannelAccountView;
import io.contentpublisher.platform.web.dto.PublicationMatrixCell;
import io.contentpublisher.platform.web.dto.PublicationMatrixRow;
import io.contentpublisher.platform.web.security.RequestActorProvider;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class PortalPublishingController {
    private final PublishingApplicationService publishing;
    private final ProjectApplicationService projects;
    private final JobApplicationService jobs;
    private final RequestActorProvider actors;

    public PortalPublishingController(PublishingApplicationService publishing,
                                      ProjectApplicationService projects,
                                      JobApplicationService jobs,
                                      RequestActorProvider actors) {
        this.publishing = publishing;
        this.projects = projects;
        this.jobs = jobs;
        this.actors = actors;
    }

    @GetMapping("/publishing")
    public String publishing(Model model) {
        var actor = actors.currentActor();
        var articles = projects.listArticles(actor, 50);
        var records = publishing.listPublicationRecords(actor, 100);
        var matrixRecords = publishing.listPublicationRecordsForArticles(actor,
                articles.stream().map(article -> article.id()).toList());
        Map<ArticleChannelKey, PublicationRecord> latest = new java.util.HashMap<>();
        matrixRecords.forEach(record -> latest.putIfAbsent(
                new ArticleChannelKey(record.articleId(), record.channelType()), record));
        var matrix = articles.stream().map(article -> new PublicationMatrixRow(article, ChannelCatalog.all().stream()
                .map(channel -> new PublicationMatrixCell(channel,
                        latest.get(new ArticleChannelKey(article.id(), channel.type())))).toList())).toList();
        Map<UUID, String> articleNames = new java.util.HashMap<>();
        articles.forEach(article -> articleNames.put(article.id(), article.title()));
        model.addAttribute("articles", articles);
        model.addAttribute("accounts", publishing.listAccounts(actor).stream().map(ChannelAccountView::from).toList());
        model.addAttribute("publicationRecords", records);
        model.addAttribute("publicationMatrix", matrix);
        model.addAttribute("articleNames", articleNames);
        model.addAttribute("publishedCount", records.stream()
                .filter(record -> record.status() == PublicationStatus.PUBLISHED).count());
        model.addAttribute("failedCount", records.stream()
                .filter(record -> record.status() == PublicationStatus.FAILED).count());
        model.addAttribute("apiRecordCount", records.stream()
                .filter(record -> record.method().name().equals("API")).count());
        model.addAttribute("manualRecordCount", records.stream()
                .filter(record -> record.method().name().equals("MANUAL")).count());
        model.addAttribute("manualTargets", ChannelCatalog.manualOnly());
        model.addAttribute("channelNames", channelNames());
        return "publishing";
    }

    @GetMapping("/channels")
    public String channels(Model model) {
        if (!model.containsAttribute("channelAccountForm")) {
            CreateChannelAccountForm form = new CreateChannelAccountForm();
            form.setIdempotencyKey(idempotencyKey("channel"));
            model.addAttribute("channelAccountForm", form);
        }
        populateChannels(model);
        return "channels";
    }

    @PostMapping("/channels")
    public String createChannel(@Valid @ModelAttribute("channelAccountForm") CreateChannelAccountForm form,
                                BindingResult bindingResult, Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateChannels(model);
            return "channels";
        }
        try {
            publishing.createAccount(actors.currentActor(), form.getType(), form.getDisplayName(),
                    blankToNull(form.getBaseUrl()), credentials(form), form.getIdempotencyKey());
            redirectAttributes.addFlashAttribute("success", "渠道账号已加密保存，可以用于自动发布");
            return "redirect:/channels";
        } catch (ApplicationException | IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            populateChannels(model);
            return "channels";
        }
    }

    @PostMapping("/articles/{articleId}/publications")
    public String publishByApi(@PathVariable UUID articleId,
                               @Valid @ModelAttribute PublishArticleForm form,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", firstError(bindingResult));
            return "redirect:/articles/" + articleId;
        }
        try {
            var job = jobs.submitPublication(actors.currentActor(), articleId, form.getChannelAccountId(),
                    blankToNull(form.getCanonicalUrl()), form.getIdempotencyKey());
            return "redirect:/jobs/" + job.id();
        } catch (ApplicationException | IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/articles/" + articleId;
        }
    }

    @PostMapping("/articles/{articleId}/publication-batches")
    public String publishBatch(@PathVariable UUID articleId,
                               @Valid @ModelAttribute BatchPublishArticleForm form,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", firstError(bindingResult));
            return "redirect:/articles/" + articleId;
        }
        try {
            var submitted = jobs.submitPublications(actors.currentActor(), articleId, form.getChannelAccountIds(),
                    blankToNull(form.getCanonicalUrl()), form.getIdempotencyKey());
            redirectAttributes.addFlashAttribute("success", "已提交 " + submitted.size() + " 个平台发布任务");
        } catch (ApplicationException | IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/articles/" + articleId;
    }

    @GetMapping("/articles/{articleId}/manual/{channelType}")
    public String manualPublish(@PathVariable UUID articleId, @PathVariable ChannelType channelType, Model model) {
        var actor = actors.currentActor();
        var article = publishing.getArticle(actor, articleId);
        var definition = ChannelCatalog.definition(channelType);
        if (!definition.manualAvailable()) {
            throw new ApplicationException("MANUAL_PUBLISH_UNAVAILABLE", "该渠道尚未配置人工发布入口");
        }
        var adapted = publishing.adaptContent(actor, articleId, channelType, null);
        if (!model.containsAttribute("manualPublicationForm")) {
            ManualPublicationForm form = new ManualPublicationForm();
            form.setContentFormat(adapted.format());
            form.setTitle(adapted.title());
            form.setContent(adapted.body());
            model.addAttribute("manualPublicationForm", form);
        }
        model.addAttribute("article", article);
        model.addAttribute("channel", definition);
        model.addAttribute("adapted", adapted);
        model.addAttribute("history", publishing.getManualPublications(actor, articleId).stream()
                .filter(item -> item.channelType() == channelType).toList());
        return "manual-publish";
    }

    @PostMapping("/articles/{articleId}/manual/{channelType}")
    public String completeManualPublish(@PathVariable UUID articleId, @PathVariable ChannelType channelType,
                                        @Valid @ModelAttribute("manualPublicationForm") ManualPublicationForm form,
                                        BindingResult bindingResult, Model model,
                                        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return manualPublish(articleId, channelType, model);
        }
        try {
            publishing.completeManualPublication(actors.currentActor(), articleId, channelType, form.getTitle(),
                    form.getContent(), form.getContentFormat(), form.getExternalUrl());
            redirectAttributes.addFlashAttribute("success", "人工发布结果已记录，内容快照和外链已留档");
            return "redirect:/articles/" + articleId;
        } catch (ApplicationException | IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            return manualPublish(articleId, channelType, model);
        }
    }

    private void populateChannels(Model model) {
        model.addAttribute("accounts", publishing.listAccounts(actors.currentActor()).stream()
                .map(ChannelAccountView::from).toList());
        model.addAttribute("apiChannels", ChannelCatalog.automated());
        model.addAttribute("manualChannels", ChannelCatalog.manualOnly());
        model.addAttribute("channelNames", channelNames());
    }

    private Map<String, String> credentials(CreateChannelAccountForm form) {
        List<String> keys = credentialKeys(form.getType());
        List<String> values = List.of(value(form.getCredentialOne()), value(form.getCredentialTwo()),
                value(form.getCredentialThree()));
        Map<String, String> credentials = new LinkedHashMap<>();
        for (int index = 0; index < keys.size(); index++) credentials.put(keys.get(index), values.get(index));
        return credentials;
    }

    private List<String> credentialKeys(ChannelType type) {
        return switch (type) {
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
            case XIAOHONGSHU, CSDN, JUEJIN, ZHIHU, CNBLOGS, SEGMENTFAULT, V2EX, OSCHINA,
                    LINKEDIN, WECHAT_OFFICIAL, JIANSHU, TOUTIAO, BILIBILI_COLUMN, BLOG_51CTO,
                    TENCENT_CLOUD, ALIBABA_CLOUD, HUAWEI_CLOUD -> List.of();
        };
    }

    private Map<ChannelType, String> channelNames() {
        Map<ChannelType, String> names = new java.util.EnumMap<>(ChannelType.class);
        ChannelCatalog.all().forEach(channel -> names.put(channel.type(), channel.displayName()));
        return names;
    }

    private String idempotencyKey(String prefix) {
        return "portal:" + prefix + ":" + UUID.randomUUID();
    }

    private String firstError(BindingResult bindingResult) {
        var error = bindingResult.getFieldError();
        return error == null ? "表单参数校验失败" : error.getDefaultMessage();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String value(String value) {
        return value == null ? "" : value.trim();
    }

    private record ArticleChannelKey(UUID articleId, ChannelType channelType) {
    }
}
