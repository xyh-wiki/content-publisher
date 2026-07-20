package io.contentpublisher.platform.application.port;

import io.contentpublisher.platform.domain.ChannelAccount;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChannelAccountRepository {
    ChannelAccount save(ChannelAccount account);

    Optional<ChannelAccount> updateIfVersionMatches(ChannelAccount account, int expectedVersion);
    Optional<ChannelAccount> findChannelAccountById(String tenantId, UUID id);
    Optional<ChannelAccount> findChannelAccountByIdempotencyKey(String tenantId, String idempotencyKey);
    List<ChannelAccount> findAll(String tenantId);
}
