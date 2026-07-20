alter table articles add column source_type varchar(20) not null default 'GIT';
alter table articles add column source_title varchar(300);
alter table articles add column source_url varchar(2048);
alter table articles add column source_description varchar(4000);
alter table articles add column target_audience varchar(500);
alter table articles add column article_type varchar(50);
alter table articles add column knowledge_level varchar(30);
alter table articles add column source_keywords_json text not null default '[]';

alter table articles alter column project_id drop not null;

alter table articles add constraint ck_articles_source
    check ((source_type = 'GIT' and project_id is not null)
        or (source_type in ('TOPIC', 'WEBSITE') and project_id is null));

alter table articles alter column source_type drop default;
alter table articles alter column source_keywords_json drop default;

create index idx_articles_tenant_source_created
    on articles(tenant_id, source_type, created_at desc);
