-- Achievements (Phase 2). A static catalog plus a per-user unlock join table.
-- The catalog is reference data seeded here; the app only reads it and writes unlocks.

create table achievements (
    id          uuid        primary key,
    code        varchar(64) not null unique,
    title       varchar(255) not null,
    description text,
    icon        varchar(16)
);

create table user_achievements (
    id             uuid        primary key,
    user_id        uuid        not null references users (id),
    achievement_id uuid        not null references achievements (id),
    unlocked_at    timestamptz not null,
    constraint uq_user_achievement unique (user_id, achievement_id)
);
create index idx_user_achievements_user on user_achievements (user_id);

insert into achievements (id, code, title, description) values
    (gen_random_uuid(), 'FIRST_TASK', 'First step',      'Complete your first task'),
    (gen_random_uuid(), 'STREAK_3',   'Getting started', 'Reach a 3-day streak'),
    (gen_random_uuid(), 'STREAK_7',   'One week strong', 'Reach a 7-day streak'),
    (gen_random_uuid(), 'STREAK_30',  'Unstoppable',     'Reach a 30-day streak'),
    (gen_random_uuid(), 'STREAK_100', 'Centurion',       'Reach a 100-day streak');
