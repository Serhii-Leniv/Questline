-- Public roadmap templates (Phase 3). Publishing a goal copies its structure here (no per-user
-- data); other users browse and import templates into their own goals.

create table goal_templates (
    id         uuid         primary key,
    author_id  uuid         references users (id),
    title      varchar(255) not null,
    summary    text,
    plan       jsonb        not null,
    task_count integer      not null default 0,
    created_at timestamptz
);
create index idx_goal_templates_created on goal_templates (created_at desc);
