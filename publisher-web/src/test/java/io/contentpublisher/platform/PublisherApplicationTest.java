package io.contentpublisher.platform;

import io.contentpublisher.platform.application.port.ChannelPublisher;
import io.contentpublisher.platform.application.ChannelCatalog;
import io.contentpublisher.platform.domain.ChannelType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:publisher;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "publisher.ai.enabled=false"
})
class PublisherApplicationTest {
    @Autowired List<ChannelPublisher> channelPublishers;

    @Test
    void contextLoadsWithDatabaseMigration() {
        assertThat(channelPublishers).extracting(ChannelPublisher::channelType)
                .containsExactlyInAnyOrderElementsOf(ChannelCatalog.automated().stream()
                        .map(ChannelCatalog.ChannelDefinition::type).toList());
    }
}
