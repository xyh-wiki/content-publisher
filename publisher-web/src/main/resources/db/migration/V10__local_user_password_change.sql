alter table local_users
    add column must_change_password boolean not null default true;
