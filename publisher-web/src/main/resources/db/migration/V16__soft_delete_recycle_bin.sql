alter table articles add column deleted_at timestamp with time zone;
alter table articles add column deleted_by varchar(200);
create index idx_articles_tenant_deleted on articles(tenant_id, deleted_at desc);

alter table jobs add column deleted_at timestamp with time zone;
alter table jobs add column deleted_by varchar(200);
create index idx_jobs_tenant_deleted on jobs(tenant_id, deleted_at desc);

alter table publications add column deleted_at timestamp with time zone;
alter table publications add column deleted_by varchar(200);
create index idx_publications_tenant_deleted on publications(tenant_id, deleted_at desc);

alter table manual_publications add column deleted_at timestamp with time zone;
alter table manual_publications add column deleted_by varchar(200);
create index idx_manual_publications_tenant_deleted on manual_publications(tenant_id, deleted_at desc);
