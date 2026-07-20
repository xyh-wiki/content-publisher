package io.contentpublisher.platform.web.dto;

import io.contentpublisher.platform.domain.Article;

import java.util.List;

public record PublicationMatrixRow(
        Article article,
        List<PublicationMatrixCell> cells) {
    public PublicationMatrixRow {
        cells = List.copyOf(cells);
    }
}
