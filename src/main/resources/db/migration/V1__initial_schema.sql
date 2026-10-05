create table app_user (
  id uuid primary key,
  email varchar(320) not null unique,
  display_name varchar(120) not null,
  created_at timestamp with time zone not null
);

create table expense_group (
  id uuid primary key,
  name varchar(120) not null,
  currency_code varchar(3) not null,
  created_by uuid not null references app_user(id),
  created_at timestamp with time zone not null
);

create table group_member (
  id uuid primary key,
  group_id uuid not null references expense_group(id),
  user_id uuid not null references app_user(id),
  joined_at timestamp with time zone not null,
  unique (group_id, user_id)
);

create table expense (
  id uuid primary key,
  group_id uuid not null references expense_group(id),
  payer_id uuid not null references app_user(id),
  creator_id uuid not null references app_user(id),
  description varchar(240) not null,
  amount_minor bigint not null,
  spent_on date not null,
  reversed boolean not null default false,
  version bigint not null default 0,
  request_key varchar(100),
  created_at timestamp with time zone not null,
  updated_at timestamp with time zone not null,
  unique (group_id, request_key),
  check (amount_minor > 0)
);

create table expense_share (
  id uuid primary key,
  expense_id uuid not null references expense(id),
  user_id uuid not null references app_user(id),
  amount_minor bigint not null,
  unique (expense_id, user_id),
  check (amount_minor >= 0)
);

create table settlement (
  id uuid primary key,
  group_id uuid not null references expense_group(id),
  sender_id uuid not null references app_user(id),
  recipient_id uuid not null references app_user(id),
  amount_minor bigint not null,
  status varchar(20) not null,
  request_key varchar(100),
  created_at timestamp with time zone not null,
  decided_at timestamp with time zone,
  unique (group_id, request_key),
  check (amount_minor > 0),
  check (sender_id <> recipient_id)
);

create table activity_event (
  id uuid primary key,
  group_id uuid not null references expense_group(id),
  actor_id uuid not null references app_user(id),
  event_type varchar(40) not null,
  subject_id uuid not null,
  detail varchar(2000) not null,
  created_at timestamp with time zone not null
);

create index idx_expense_group on expense(group_id, created_at);
create index idx_share_expense on expense_share(expense_id);
create index idx_settlement_group on settlement(group_id, created_at);
create index idx_activity_group on activity_event(group_id, created_at);
