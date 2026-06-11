-- Topic tags on tasks, for tracking which areas a user has covered (Phase 2).

create table topics (
    id      uuid         primary key,
    user_id uuid         not null references users (id),
    name    varchar(128) not null,
    slug    varchar(128) not null,
    constraint uq_topics_user_slug unique (user_id, slug)
);

create table task_topics (
    task_id  uuid not null references tasks (id) on delete cascade,
    topic_id uuid not null references topics (id) on delete cascade,
    primary key (task_id, topic_id)
);
