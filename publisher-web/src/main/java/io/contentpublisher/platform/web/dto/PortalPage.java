package io.contentpublisher.platform.web.dto;

import java.util.List;

public record PortalPage<T>(List<T> items, int page, int pageSize, long totalItems, int totalPages) {
    public PortalPage {
        items = List.copyOf(items);
    }

    public static <T> PortalPage<T> from(List<T> source, int requestedPage, int requestedPageSize) {
        int pageSize = Math.max(10, Math.min(requestedPageSize, 50));
        int totalPages = source.isEmpty() ? 1 : (int) Math.ceil(source.size() / (double) pageSize);
        int page = Math.max(0, Math.min(requestedPage, totalPages - 1));
        int fromIndex = Math.min(page * pageSize, source.size());
        int toIndex = Math.min(fromIndex + pageSize, source.size());
        return new PortalPage<>(source.subList(fromIndex, toIndex), page, pageSize, source.size(), totalPages);
    }

    public boolean hasPrevious() {
        return page > 0;
    }

    public boolean hasNext() {
        return page + 1 < totalPages;
    }

    public int previousPage() {
        return Math.max(0, page - 1);
    }

    public int nextPage() {
        return Math.min(totalPages - 1, page + 1);
    }
}
