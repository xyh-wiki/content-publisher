package io.contentpublisher.platform.application.port;

import io.contentpublisher.platform.domain.WebsiteSnapshot;

public interface WebsiteInspector {
    WebsiteSnapshot inspect(String websiteUrl);
}
