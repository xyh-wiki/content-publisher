package io.contentpublisher.platform.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
class ArticleVersionKey implements Serializable {
    @Column(name = "article_id") UUID articleId;
    @Column(name = "version_number") int versionNumber;

    protected ArticleVersionKey() {}

    ArticleVersionKey(UUID articleId, int versionNumber) {
        this.articleId = articleId;
        this.versionNumber = versionNumber;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ArticleVersionKey key)) return false;
        return versionNumber == key.versionNumber && Objects.equals(articleId, key.articleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(articleId, versionNumber);
    }
}
