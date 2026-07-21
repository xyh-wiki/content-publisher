package io.contentpublisher.platform.web.controller;

import io.contentpublisher.platform.application.ApplicationException;
import io.contentpublisher.platform.application.ProjectApplicationService;
import io.contentpublisher.platform.application.PublishingApplicationService;
import io.contentpublisher.platform.domain.Article;
import io.contentpublisher.platform.domain.ArticleStatus;
import io.contentpublisher.platform.web.dto.ArticleSeoView;
import io.contentpublisher.platform.web.form.RejectArticleForm;
import io.contentpublisher.platform.web.form.UpdateArticleForm;
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

import java.util.UUID;

import static io.contentpublisher.platform.web.controller.PortalFormSupport.firstError;
import static io.contentpublisher.platform.web.controller.PortalFormSupport.splitValues;

@Controller
public class ContentLibraryPortalController {
    private static final int LIST_LIMIT = 50;

    private final ProjectApplicationService projects;
    private final PublishingApplicationService publishing;
    private final RequestActorProvider actors;

    public ContentLibraryPortalController(ProjectApplicationService projects,
                                          PublishingApplicationService publishing,
                                          RequestActorProvider actors) {
        this.projects = projects;
        this.publishing = publishing;
        this.actors = actors;
    }

    @GetMapping("/content")
    public String content(Model model) {
        model.addAttribute("articles", projects.listArticles(actors.currentActor(), LIST_LIMIT));
        return "content-records";
    }

    @GetMapping("/articles/{articleId}")
    public String article(@PathVariable UUID articleId, Model model) {
        var actor = actors.currentActor();
        Article article = publishing.getArticle(actor, articleId);
        model.addAttribute("article", article);
        model.addAttribute("seo", ArticleSeoView.from(article));
        model.addAttribute("versions", publishing.getArticleVersions(actor, articleId));
        model.addAttribute("updateArticleForm", articleForm(article));
        model.addAttribute("rejectArticleForm", new RejectArticleForm());
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
                    form.getSummary(), form.getMarkdown(), splitValues(form.getTags()), splitValues(form.getKeywords()),
                    form.getTitleEn(), form.getSummaryEn(), form.getMarkdownEn(), splitValues(form.getTagsEn()),
                    splitValues(form.getKeywordsEn()));
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

    private UpdateArticleForm articleForm(Article article) {
        UpdateArticleForm form = new UpdateArticleForm();
        form.setExpectedVersion(article.currentVersion());
        form.setTitle(article.title());
        form.setSummary(article.summary());
        form.setMarkdown(article.markdown());
        form.setTags(String.join("\n", article.tags()));
        form.setKeywords(String.join("\n", article.keywords()));
        form.setTitleEn(article.titleEn());
        form.setSummaryEn(article.summaryEn());
        form.setMarkdownEn(article.markdownEn());
        form.setTagsEn(String.join("\n", article.tagsEn()));
        form.setKeywordsEn(String.join("\n", article.keywordsEn()));
        return form;
    }
}
