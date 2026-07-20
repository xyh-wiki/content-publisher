create table projects (
    id uuid primary key,
    git_url varchar(2048) not null,
    name varchar(255) not null,
    description varchar(2000),
    default_branch varchar(255),
    revision varchar(64),
    languages_json text not null,
    license varchar(100),
    status varchar(30) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_projects_git_url unique (git_url)
);

create table repository_snapshots (
    project_id uuid primary key references projects(id) on delete cascade,
    name varchar(255) not null,
    description varchar(2000),
    default_branch varchar(255),
    revision varchar(64) not null,
    readme text not null,
    manifest_summary text not null,
    file_tree_json text not null,
    languages_json text not null,
    license varchar(100)
);

create table articles (
    id uuid primary key,
    project_id uuid not null references projects(id),
    title varchar(500) not null,
    summary varchar(2000) not null,
    markdown text not null,
    keywords_json text not null,
    language varchar(20) not null,
    source_revision varchar(64) not null,
    status varchar(30) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_articles_project_created on articles(project_id, created_at desc);
