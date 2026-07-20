alter table articles add column current_version integer not null default 1;

create table article_versions (
    article_id uuid not null references articles(id) on delete cascade,
    version_number integer not null,
    tenant_id varchar(100) not null,
    title varchar(500) not null,
    summary varchar(2000) not null,
    markdown text not null,
    keywords_json text not null,
    created_by varchar(200) not null,
    created_at timestamp with time zone not null,
    primary key (article_id, version_number),
    constraint ck_article_versions_number check (version_number >= 1)
);

insert into article_versions (
    article_id, version_number, tenant_id, title, summary, markdown,
    keywords_json, created_by, created_at
)
select id, 1, tenant_id, title, summary, markdown, keywords_json, created_by, created_at
from articles;

create index idx_article_versions_tenant_article on article_versions(tenant_id, article_id, version_number desc);

alter table articles alter column current_version drop default;
