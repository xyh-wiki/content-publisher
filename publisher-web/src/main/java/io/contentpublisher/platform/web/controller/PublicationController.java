package io.contentpublisher.platform.web.controller;

import io.contentpublisher.platform.application.PublishingApplicationService;
import io.contentpublisher.platform.web.dto.PublicationRecordResponse;
import io.contentpublisher.platform.web.dto.PublicationResponse;
import io.contentpublisher.platform.web.security.RequestActorProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/publications")
public class PublicationController {
    private final PublishingApplicationService publishing;
    private final RequestActorProvider actors;

    public PublicationController(PublishingApplicationService publishing, RequestActorProvider actors) {
        this.publishing = publishing;
        this.actors = actors;
    }

    @GetMapping("/{publicationId}")
    public PublicationResponse get(@PathVariable UUID publicationId) {
        return PublicationResponse.from(publishing.getPublication(actors.currentActor(), publicationId));
    }

    @GetMapping
    public List<PublicationRecordResponse> list(@RequestParam(defaultValue = "50") int limit) {
        return publishing.listPublicationRecords(actors.currentActor(), limit).stream()
                .map(PublicationRecordResponse::from).toList();
    }
}
