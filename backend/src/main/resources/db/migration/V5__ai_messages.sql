-- Chat messages for refining an AI plan before it is accepted (Flow A refine).

create table ai_messages (
    id         uuid        primary key,
    goal_id    uuid        not null references goals (id) on delete cascade,
    job_id     uuid,
    role       varchar(16) not null,
    content    text        not null,
    created_at timestamptz
);
create index idx_ai_messages_goal on ai_messages (goal_id);
