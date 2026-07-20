create table manual_publications (
    id uuid primary key,
    tenant_id varchar(100) not null,
    article_id uuid not null references articles(id),
    channel_type varchar(40) not null,
    content_format varchar(30) not null,
    adapted_title varchar(500) not null,
    adapted_content text not null,
    external_url varchar(2048) not null,
    published_by varchar(200) not null,
    published_at timestamp with time zone not null
);

create index idx_manual_publications_tenant_article
    on manual_publications(tenant_id, article_id, published_at desc);

create index idx_manual_publications_tenant_published
    on manual_publications(tenant_id, published_at desc);
