package io.contentpublisher.platform.web.controller;

import io.contentpublisher.platform.application.ApplicationException;
import io.contentpublisher.platform.application.RecordManagementApplicationService;
import io.contentpublisher.platform.web.security.RequestActorProvider;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
public class RecycleBinPortalController {
    private static final int LIST_LIMIT = 50;

    private final RecordManagementApplicationService records;
    private final RequestActorProvider actors;

    public RecycleBinPortalController(RecordManagementApplicationService records, RequestActorProvider actors) {
        this.records = records;
        this.actors = actors;
    }

    @GetMapping("/recycle-bin")
    public String recycleBin(Model model) {
        var actor = actors.currentActor();
        model.addAttribute("deletedArticles", records.listDeletedArticles(actor, LIST_LIMIT));
        model.addAttribute("deletedJobs", records.listDeletedJobs(actor, LIST_LIMIT));
        return "recycle-bin";
    }

    @PostMapping("/articles/{articleId}/delete")
    public String deleteArticle(@PathVariable UUID articleId, RedirectAttributes redirectAttributes) {
        try {
            records.deleteArticleRecord(actors.currentActor(), articleId);
            redirectAttributes.addFlashAttribute("success", "内容及关联记录已移入回收站，可由管理员恢复");
        } catch (ApplicationException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/content";
    }

    @PostMapping("/jobs/{jobId}/delete")
    public String deleteJob(@PathVariable UUID jobId, RedirectAttributes redirectAttributes) {
        try {
            records.deleteJobRecord(actors.currentActor(), jobId);
            redirectAttributes.addFlashAttribute("success", "任务已移入回收站，可由管理员恢复");
        } catch (ApplicationException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/jobs";
    }

    @PostMapping("/articles/{articleId}/restore")
    public String restoreArticle(@PathVariable UUID articleId, RedirectAttributes redirectAttributes) {
        try {
            records.restoreArticleRecord(actors.currentActor(), articleId);
            redirectAttributes.addFlashAttribute("success", "内容及关联记录已恢复");
        } catch (ApplicationException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/recycle-bin";
    }

    @PostMapping("/jobs/{jobId}/restore")
    public String restoreJob(@PathVariable UUID jobId, RedirectAttributes redirectAttributes) {
        try {
            records.restoreJobRecord(actors.currentActor(), jobId);
            redirectAttributes.addFlashAttribute("success", "任务记录已恢复");
        } catch (ApplicationException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/recycle-bin";
    }
}
