alter table projects add column tenant_id varchar(100) not null default 'legacy';
alter table projects add column created_by varchar(200) not null default 'system-migration';
alter table projects add column updated_by varchar(200) not null default 'system-migration';
alter table projects drop constraint uk_projects_git_url;
alter table projects add constraint uk_projects_tenant_git_url unique (tenant_id, git_url);
create index idx_projects_tenant_updated on projects(tenant_id, updated_at desc);

alter table repository_snapshots add column tenant_id varchar(100) not null default 'legacy';
create index idx_snapshots_tenant_project on repository_snapshots(tenant_id, project_id);

alter table articles add column tenant_id varchar(100) not null default 'legacy';
alter table articles add column created_by varchar(200) not null default 'system-migration';
alter table articles add column updated_by varchar(200) not null default 'system-migration';
create index idx_articles_tenant_project_created on articles(tenant_id, project_id, created_at desc);

create table audit_logs (
    id uuid primary key,
    tenant_id varchar(100) not null,
    subject varchar(200) not null,
    action varchar(100) not null,
    target_type varchar(100) not null,
    target_id uuid not null,
    details_json text not null,
    occurred_at timestamp with time zone not null
);

create index idx_audit_tenant_occurred on audit_logs(tenant_id, occurred_at desc);
create index idx_audit_target on audit_logs(tenant_id, target_type, target_id);

alter table projects alter column tenant_id drop default;
alter table projects alter column created_by drop default;
alter table projects alter column updated_by drop default;
alter table repository_snapshots alter column tenant_id drop default;
alter table articles alter column tenant_id drop default;
alter table articles alter column created_by drop default;
alter table articles alter column updated_by drop default;
