package io.contentpublisher.platform.web.controller;

import io.contentpublisher.platform.application.port.MarkdownRenderer;
import io.contentpublisher.platform.web.dto.MarkdownPreviewRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/markdown")
public class MarkdownPreviewController {
    private final MarkdownRenderer renderer;

    public MarkdownPreviewController(MarkdownRenderer renderer) {
        this.renderer = renderer;
    }

    @PostMapping("/preview")
    public Map<String, String> preview(@Valid @RequestBody MarkdownPreviewRequest request) {
        return Map.of("html", renderer.render(request.markdown()));
    }
}
