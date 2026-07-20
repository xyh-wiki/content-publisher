alter table articles add column generation_job_id uuid;
alter table articles add constraint uk_articles_generation_job unique (generation_job_id);

create table jobs (
    id uuid primary key,
    tenant_id varchar(100) not null,
    actor_subject varchar(200) not null,
    type varchar(40) not null,
    status varchar(30) not null,
    payload_json text not null,
    idempotency_key varchar(128) not null,
    request_hash varchar(64) not null,
    attempt integer not null,
    max_attempts integer not null,
    scheduled_at timestamp with time zone not null,
    locked_at timestamp with time zone,
    lock_owner varchar(200),
    result_resource_id uuid,
    error_code varchar(100),
    error_message varchar(2000),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    version bigint not null default 0,
    constraint uk_jobs_tenant_idempotency unique (tenant_id, idempotency_key),
    constraint ck_jobs_attempts check (attempt >= 0 and attempt <= max_attempts and max_attempts >= 1)
);

create index idx_jobs_claim on jobs(status, scheduled_at, created_at);
create index idx_jobs_tenant_created on jobs(tenant_id, created_at desc);
create index idx_jobs_stale_lock on jobs(status, locked_at);
