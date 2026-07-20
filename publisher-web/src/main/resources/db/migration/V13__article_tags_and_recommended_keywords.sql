alter table articles add column tags_json text not null default '[]';
update articles set tags_json = keywords_json;
alter table articles alter column tags_json drop default;

alter table article_versions add column tags_json text not null default '[]';
update article_versions set tags_json = keywords_json;
alter table article_versions alter column tags_json drop default;
