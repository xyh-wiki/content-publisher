alter table channel_accounts add column verification_status varchar(20);
alter table channel_accounts add column verification_message varchar(500);
alter table channel_accounts add column last_verified_at timestamp with time zone;

alter table channel_accounts add constraint ck_channel_accounts_verification_status
    check (verification_status is null or verification_status in ('SUCCEEDED', 'FAILED'));
