create table group_invite (
  id uuid primary key,
  group_id uuid not null references expense_group(id),
  invited_email varchar(320) not null,
  token_hash varchar(64) not null unique,
  invited_by uuid not null references app_user(id),
  expires_at timestamp with time zone not null,
  accepted_by uuid references app_user(id),
  accepted_at timestamp with time zone,
  created_at timestamp with time zone not null
);

create index idx_invite_group on group_invite(group_id, created_at);
