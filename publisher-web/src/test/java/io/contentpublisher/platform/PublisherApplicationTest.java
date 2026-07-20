package io.contentpublisher.platform;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:publisher;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "publisher.ai.enabled=false"
})
class PublisherApplicationTest {
    @Test
    void contextLoadsWithDatabaseMigration() {
    }
}
