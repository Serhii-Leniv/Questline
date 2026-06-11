-- AI job tracking (Phase 1, Slice 2). Durable record of each LLM operation; the UI polls it.
-- input/output are JSON (the ai/ records serialized to maps). ai_messages (chat refine) arrives
-- with its own migration in Phase 2.

create table ai_jobs (
    id            uuid        primary key,
    user_id       uuid        not null references users (id),
    goal_id       uuid        references goals (id) on delete cascade,
    type          varchar(32) not null,
    status        varchar(32) not null default 'PENDING',
    input         jsonb,
    output        jsonb,
    error         text,
    tokens_input  integer,
    tokens_output integer,
    attempts      integer     not null default 0,
    created_at    timestamptz,
    finished_at   timestamptz
);
create index idx_ai_jobs_user_type_status on ai_jobs (user_id, type, status);
create index idx_ai_jobs_goal on ai_jobs (goal_id);
