alter table articles add column title_en varchar(500) not null default '';
alter table articles add column summary_en varchar(2000) not null default '';
alter table articles add column markdown_en text not null default '';
alter table articles add column tags_en_json text not null default '[]';
alter table articles add column keywords_en_json text not null default '[]';

alter table article_versions add column title_en varchar(500) not null default '';
alter table article_versions add column summary_en varchar(2000) not null default '';
alter table article_versions add column markdown_en text not null default '';
alter table article_versions add column tags_en_json text not null default '[]';
alter table article_versions add column keywords_en_json text not null default '[]';
