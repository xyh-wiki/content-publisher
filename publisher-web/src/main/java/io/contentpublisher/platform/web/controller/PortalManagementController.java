package io.contentpublisher.platform.web.controller;

import io.contentpublisher.platform.application.ApplicationException;
import io.contentpublisher.platform.application.AiSettingsApplicationService;
import io.contentpublisher.platform.application.JobApplicationService;
import io.contentpublisher.platform.application.ProjectApplicationService;
import io.contentpublisher.platform.application.PublishingApplicationService;
import io.contentpublisher.platform.application.ChannelCatalog;
import io.contentpublisher.platform.domain.Article;
import io.contentpublisher.platform.domain.ArticleStatus;
import io.contentpublisher.platform.domain.GenerationPolicy;
import io.contentpublisher.platform.domain.Job;
import io.contentpublisher.platform.domain.JobStatus;
import io.contentpublisher.platform.domain.TopicBrief;
import io.contentpublisher.platform.domain.WebsiteBrief;
import io.contentpublisher.platform.infrastructure.config.AiProperties;
import io.contentpublisher.platform.web.form.GenerateArticleForm;
import io.contentpublisher.platform.web.form.ImportProjectForm;
import io.contentpublisher.platform.web.form.RejectArticleForm;
import io.contentpublisher.platform.web.form.UpdateArticleForm;
import io.contentpublisher.platform.web.form.PublishArticleForm;
import io.contentpublisher.platform.web.form.CreateTopicArticleForm;
import io.contentpublisher.platform.web.form.CreateWebsiteArticleForm;
import io.contentpublisher.platform.web.dto.ChannelAccountView;
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

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Controller
public class PortalManagementController {
    private static final int LIST_LIMIT = 50;

    private final ProjectApplicationService projects;
    private final JobApplicationService jobs;
    private final PublishingApplicationService publishing;
    private final RequestActorProvider actors;
    private final AiProperties aiProperties;
    private final AiSettingsApplicationService aiSettings;

    public PortalManagementController(ProjectApplicationService projects, JobApplicationService jobs,
                                      PublishingApplicationService publishing, RequestActorProvider actors,
                                      AiProperties aiProperties, AiSettingsApplicationService aiSettings) {
        this.projects = projects;
        this.jobs = jobs;
        this.publishing = publishing;
        this.actors = actors;
        this.aiProperties = aiProperties;
        this.aiSettings = aiSettings;
    }

    @GetMapping("/projects")
    public String projects(Model model) {
        activateSource(model, "git");
        populateSourceForms(model);
        populateWorkspace(model);
        return "projects";
    }

    @PostMapping("/articles/topic-generations")
    public String generateTopicArticle(@Valid @ModelAttribute("topicArticleForm") CreateTopicArticleForm form,
                                       BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            activateSource(model, "topic");
            populateSourceForms(model);
            populateWorkspace(model);
            return "projects";
        }
        try {
            List<String> keywords = splitValues(form.getKeywords());
            TopicBrief brief = new TopicBrief(form.getTopic(), form.getDescription(), form.getAudience(),
                    form.getArticleType(), form.getKnowledgeLevel(), keywords, trimToNull(form.getReferenceNotes()));
            GenerationPolicy policy = new GenerationPolicy(form.getLanguage(), form.getTone(),
                    form.getMinCharacters(), form.getMaxCharacters(), form.getMaxKeywords(), keywords,
                    splitValues(form.getExcludedKeywords()), splitValues(form.getRequiredSections()));
            Job job = jobs.submitTopicArticleGeneration(actors.currentActor(), brief, policy, form.getIdempotencyKey());
            return "redirect:/jobs/" + job.id();
        } catch (ApplicationException | IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            activateSource(model, "topic");
            populateSourceForms(model);
            populateWorkspace(model);
            return "projects";
        }
    }

    @PostMapping("/articles/website-generations")
    public String generateWebsiteArticle(@Valid @ModelAttribute("websiteArticleForm") CreateWebsiteArticleForm form,
                                         BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            activateSource(model, "website");
            populateSourceForms(model);
            populateWorkspace(model);
            return "projects";
        }
        try {
            List<String> keywords = splitValues(form.getKeywords());
            WebsiteBrief brief = new WebsiteBrief(form.getWebsiteUrl(), form.getRecommendationAngle(),
                    form.getAudience(), keywords);
            GenerationPolicy policy = new GenerationPolicy(form.getLanguage(), form.getTone(),
                    form.getMinCharacters(), form.getMaxCharacters(), form.getMaxKeywords(), keywords,
                    splitValues(form.getExcludedKeywords()), splitValues(form.getRequiredSections()));
            Job job = jobs.submitWebsiteArticleGeneration(actors.currentActor(), brief, policy,
                    form.getIdempotencyKey());
            return "redirect:/jobs/" + job.id();
        } catch (ApplicationException | IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            activateSource(model, "website");
            populateSourceForms(model);
            populateWorkspace(model);
            return "projects";
        }
    }

    @PostMapping("/projects/imports")
    public String importProject(@Valid @ModelAttribute("importProjectForm") ImportProjectForm form,
                                BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            activateSource(model, "git");
            populateSourceForms(model);
            populateWorkspace(model);
            return "projects";
        }
        try {
            Job job = jobs.submitImport(actors.currentActor(), form.getGitUrl().trim(), trimToNull(form.getBranch()),
                    form.getIdempotencyKey());
            return "redirect:/jobs/" + job.id();
        } catch (ApplicationException | IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            activateSource(model, "git");
            populateSourceForms(model);
            populateWorkspace(model);
            return "projects";
        }
    }

    @GetMapping("/projects/{projectId}")
    public String project(@PathVariable UUID projectId, Model model) {
        var actor = actors.currentActor();
        model.addAttribute("project", projects.getProject(actor, projectId));
        model.addAttribute("articles", projects.listProjectArticles(actor, projectId, LIST_LIMIT));
        model.addAttribute("generateArticleForm", newGenerationForm());
        model.addAttribute("aiEnabled", aiEnabled());
        model.addAttribute("aiModel", aiModel());
        return "project-detail";
    }

    @PostMapping("/projects/{projectId}/articles")
    public String generateArticle(@PathVariable UUID projectId,
                                  @Valid @ModelAttribute("generateArticleForm") GenerateArticleForm form,
                                  BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", firstError(bindingResult));
            return "redirect:/projects/" + projectId;
        }
        if (!aiEnabled()) {
            redirectAttributes.addFlashAttribute("error", "AI 内容生成尚未启用，请先配置 AI 服务");
            return "redirect:/projects/" + projectId;
        }
        try {
            GenerationPolicy policy = new GenerationPolicy(form.getLanguage().trim(), form.getTone().trim(),
                    form.getMinCharacters(), form.getMaxCharacters(), form.getMaxKeywords(),
                    splitValues(form.getRequiredKeywords()), splitValues(form.getExcludedKeywords()),
                    splitValues(form.getRequiredSections()));
            Job job = jobs.submitArticleGeneration(actors.currentActor(), projectId, policy,
                    form.getIdempotencyKey());
            return "redirect:/jobs/" + job.id();
        } catch (ApplicationException | IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/projects/" + projectId;
        }
    }

    @GetMapping("/jobs/{jobId}")
    public String job(@PathVariable UUID jobId, Model model) {
        Job job = jobs.getJob(actors.currentActor(), jobId);
        model.addAttribute("job", job);
        model.addAttribute("active", job.status() == JobStatus.PENDING || job.status() == JobStatus.RUNNING
                || job.status() == JobStatus.RETRY_WAIT);
        model.addAttribute("resultLink", resultLink(job));
        return "job-detail";
    }

    @GetMapping("/articles/{articleId}")
    public String article(@PathVariable UUID articleId, Model model) {
        var actor = actors.currentActor();
        Article article = publishing.getArticle(actor, articleId);
        model.addAttribute("article", article);
        model.addAttribute("versions", publishing.getArticleVersions(actor, articleId));
        model.addAttribute("updateArticleForm", articleForm(article));
        model.addAttribute("rejectArticleForm", new RejectArticleForm());
        PublishArticleForm publishArticleForm = new PublishArticleForm();
        publishArticleForm.setIdempotencyKey(idempotencyKey("publish"));
        model.addAttribute("publishArticleForm", publishArticleForm);
        model.addAttribute("channelAccounts", publishing.listAccounts(actor).stream()
                .filter(account -> account.status().name().equals("ACTIVE"))
                .map(ChannelAccountView::from).toList());
        model.addAttribute("manualTargets", ChannelCatalog.all().stream()
                .filter(ChannelCatalog.ChannelDefinition::manualAvailable).toList());
        model.addAttribute("publicationRecords", publishing.getArticlePublicationRecords(actor, articleId));
        java.util.Map<io.contentpublisher.platform.domain.ChannelType, String> channelNames =
                new java.util.EnumMap<>(io.contentpublisher.platform.domain.ChannelType.class);
        ChannelCatalog.all().forEach(channel -> channelNames.put(channel.type(), channel.displayName()));
        model.addAttribute("channelNames", channelNames);
        model.addAttribute("editable", article.status() == ArticleStatus.DRAFT
                || article.status() == ArticleStatus.REJECTED);
        return "article-detail";
    }

    @PostMapping("/articles/{articleId}/edit")
    public String updateArticle(@PathVariable UUID articleId,
                                @Valid @ModelAttribute UpdateArticleForm form, BindingResult bindingResult,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", firstError(bindingResult));
            return "redirect:/articles/" + articleId;
        }
        try {
            publishing.updateArticle(actors.currentActor(), articleId, form.getExpectedVersion(), form.getTitle(),
                    form.getSummary(), form.getMarkdown(), splitValues(form.getKeywords()));
            redirectAttributes.addFlashAttribute("success", "文章已保存为新版本");
        } catch (ApplicationException | IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/articles/" + articleId;
    }

    @PostMapping("/articles/{articleId}/approve")
    public String approveArticle(@PathVariable UUID articleId, RedirectAttributes redirectAttributes) {
        try {
            publishing.approveArticle(actors.currentActor(), articleId);
            redirectAttributes.addFlashAttribute("success", "文章已审核通过");
        } catch (ApplicationException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/articles/" + articleId;
    }

    @PostMapping("/articles/{articleId}/reject")
    public String rejectArticle(@PathVariable UUID articleId,
                                @Valid @ModelAttribute RejectArticleForm form, BindingResult bindingResult,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", firstError(bindingResult));
            return "redirect:/articles/" + articleId;
        }
        try {
            publishing.rejectArticle(actors.currentActor(), articleId, form.getReason());
            redirectAttributes.addFlashAttribute("success", "文章已驳回");
        } catch (ApplicationException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/articles/" + articleId;
    }

    private void populateWorkspace(Model model) {
        var actor = actors.currentActor();
        model.addAttribute("projects", projects.listProjects(actor, LIST_LIMIT));
        model.addAttribute("jobs", jobs.listJobs(actor, 20));
        model.addAttribute("articles", projects.listArticles(actor, 20));
        model.addAttribute("aiEnabled", aiEnabled());
        model.addAttribute("aiModel", aiModel());
    }

    private void populateSourceForms(Model model) {
        if (!model.containsAttribute("importProjectForm")) {
            ImportProjectForm form = new ImportProjectForm();
            form.setIdempotencyKey(idempotencyKey("import"));
            model.addAttribute("importProjectForm", form);
        }
        if (!model.containsAttribute("topicArticleForm")) {
            CreateTopicArticleForm form = new CreateTopicArticleForm();
            form.setIdempotencyKey(idempotencyKey("topic"));
            model.addAttribute("topicArticleForm", form);
        }
        if (!model.containsAttribute("websiteArticleForm")) {
            CreateWebsiteArticleForm form = new CreateWebsiteArticleForm();
            form.setIdempotencyKey(idempotencyKey("website"));
            model.addAttribute("websiteArticleForm", form);
        }
    }

    private void activateSource(Model model, String source) {
        if (!model.containsAttribute("activeSourcePanel")) model.addAttribute("activeSourcePanel", source);
    }

    private GenerateArticleForm newGenerationForm() {
        GenerateArticleForm form = new GenerateArticleForm();
        form.setIdempotencyKey(idempotencyKey("article"));
        return form;
    }

    private UpdateArticleForm articleForm(Article article) {
        UpdateArticleForm form = new UpdateArticleForm();
        form.setExpectedVersion(article.currentVersion());
        form.setTitle(article.title());
        form.setSummary(article.summary());
        form.setMarkdown(article.markdown());
        form.setKeywords(String.join("\n", article.keywords()));
        return form;
    }

    private String resultLink(Job job) {
        if (job.status() != JobStatus.SUCCEEDED || job.resultResourceId() == null) return null;
        return switch (job.type()) {
            case IMPORT_PROJECT -> "/projects/" + job.resultResourceId();
            case GENERATE_ARTICLE, GENERATE_TOPIC_ARTICLE, GENERATE_WEBSITE_ARTICLE ->
                    "/articles/" + job.resultResourceId();
            case PUBLISH_ARTICLE -> null;
        };
    }

    private List<String> splitValues(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split("[,\\n]"))
                .map(String::trim).filter(item -> !item.isBlank()).distinct().toList();
    }

    private String firstError(BindingResult bindingResult) {
        var error = bindingResult.getFieldError();
        return error == null ? "表单参数校验失败" : error.getDefaultMessage();
    }

    private String idempotencyKey(String prefix) {
        return "portal:" + prefix + ":" + UUID.randomUUID();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean aiEnabled() {
        return aiSettings.getSettings(actors.currentActor()).map(settings -> settings.enabled())
                .orElse(aiProperties.enabled());
    }

    private String aiModel() {
        return aiSettings.getSettings(actors.currentActor()).map(settings -> settings.model())
                .orElse(aiProperties.model());
    }
}
