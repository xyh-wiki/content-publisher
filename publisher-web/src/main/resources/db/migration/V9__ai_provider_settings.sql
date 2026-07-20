create table ai_provider_settings (
    tenant_id varchar(100) primary key,
    base_url varchar(2048) not null,
    encrypted_api_key text,
    api_key_fingerprint varchar(64),
    model varchar(200) not null,
    timeout_seconds integer not null,
    temperature decimal(4, 3) not null,
    enabled boolean not null,
    settings_version integer not null,
    created_by varchar(200) not null,
    updated_by varchar(200) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint ck_ai_settings_timeout check (timeout_seconds between 5 and 300),
    constraint ck_ai_settings_temperature check (temperature between 0 and 1),
    constraint ck_ai_settings_version check (settings_version > 0)
);
