create table local_users (
    id uuid primary key,
    tenant_id varchar(100) not null,
    username varchar(100) not null,
    password_hash varchar(100) not null,
    enabled boolean not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_local_users_username unique (username)
);

create table local_user_roles (
    user_id uuid not null references local_users(id) on delete cascade,
    role varchar(30) not null,
    primary key (user_id, role)
);

create index idx_local_users_tenant on local_users(tenant_id, username);
