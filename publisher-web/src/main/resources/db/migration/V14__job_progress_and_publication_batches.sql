alter table jobs add column progress_percent integer not null default 5;
alter table jobs add column progress_label varchar(100) not null default '等待执行';
alter table jobs add column progress_detail varchar(500) not null default '任务已进入队列，等待后台工作器领取';
alter table jobs add column batch_id uuid;

alter table jobs add constraint ck_jobs_progress check (progress_percent >= 0 and progress_percent <= 100);
create index idx_jobs_tenant_batch on jobs(tenant_id, batch_id, created_at desc);
