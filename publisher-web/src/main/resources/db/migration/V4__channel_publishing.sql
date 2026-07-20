create table channel_accounts (
    id uuid primary key,
    tenant_id varchar(100) not null,
    type varchar(40) not null,
    display_name varchar(120) not null,
    base_url varchar(2048) not null,
    encrypted_credentials text not null,
    idempotency_key varchar(128) not null,
    request_hash varchar(64) not null,
    status varchar(30) not null,
    created_by varchar(200) not null,
    updated_by varchar(200) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_channel_accounts_tenant_idempotency unique (tenant_id, idempotency_key)
);

create index idx_channel_accounts_tenant_created on channel_accounts(tenant_id, created_at desc);

create table publications (
    id uuid primary key,
    tenant_id varchar(100) not null,
    article_id uuid not null references articles(id),
    channel_account_id uuid not null references channel_accounts(id),
    publication_job_id uuid not null references jobs(id),
    channel_type varchar(40) not null,
    status varchar(30) not null,
    external_id varchar(500),
    external_url varchar(2048),
    error_code varchar(100),
    error_message varchar(2000),
    published_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_publications_job unique (publication_job_id)
);

create index idx_publications_tenant_article on publications(tenant_id, article_id, created_at desc);
create index idx_publications_account_created on publications(channel_account_id, created_at desc);
