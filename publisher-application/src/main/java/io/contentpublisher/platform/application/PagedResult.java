package io.contentpublisher.platform.application;

import java.util.List;

public record PagedResult<T>(List<T> items, int page, int pageSize, long totalItems) {
    public PagedResult {
        items = List.copyOf(items);
        if (page < 0 || pageSize < 1 || totalItems < 0) throw new IllegalArgumentException("分页参数无效");
    }

    public int totalPages() {
        return totalItems == 0 ? 1 : (int) Math.ceil(totalItems / (double) pageSize);
    }

    public boolean hasPrevious() {
        return page > 0;
    }

    public boolean hasNext() {
        return page + 1 < totalPages();
    }

    public int previousPage() {
        return Math.max(0, page - 1);
    }

    public int nextPage() {
        return Math.min(totalPages() - 1, page + 1);
    }
}
