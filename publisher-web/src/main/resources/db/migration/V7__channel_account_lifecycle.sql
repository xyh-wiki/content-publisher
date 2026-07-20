alter table channel_accounts add column credential_fingerprint varchar(64);
alter table channel_accounts add column account_version integer not null default 1;

update channel_accounts set credential_fingerprint = request_hash where credential_fingerprint is null;

alter table channel_accounts alter column credential_fingerprint set not null;
alter table channel_accounts alter column account_version drop default;

alter table channel_accounts add constraint ck_channel_accounts_version check (account_version >= 1);
