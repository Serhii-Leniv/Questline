-- Account deletion (Phase 3). Make every user-owned FK cascade so deleting a user removes all
-- their data in one statement. The author link on public templates is set null (templates survive).

alter table goals drop constraint goals_user_id_fkey,
    add constraint goals_user_id_fkey foreign key (user_id) references users (id) on delete cascade;

alter table tasks drop constraint tasks_user_id_fkey,
    add constraint tasks_user_id_fkey foreign key (user_id) references users (id) on delete cascade;

alter table activity_days drop constraint activity_days_user_id_fkey,
    add constraint activity_days_user_id_fkey foreign key (user_id) references users (id) on delete cascade;

alter table streaks drop constraint streaks_user_id_fkey,
    add constraint streaks_user_id_fkey foreign key (user_id) references users (id) on delete cascade;

alter table topics drop constraint topics_user_id_fkey,
    add constraint topics_user_id_fkey foreign key (user_id) references users (id) on delete cascade;

alter table ai_jobs drop constraint ai_jobs_user_id_fkey,
    add constraint ai_jobs_user_id_fkey foreign key (user_id) references users (id) on delete cascade;

alter table user_achievements drop constraint user_achievements_user_id_fkey,
    add constraint user_achievements_user_id_fkey foreign key (user_id) references users (id) on delete cascade;

alter table goal_templates drop constraint goal_templates_author_id_fkey,
    add constraint goal_templates_author_id_fkey foreign key (author_id) references users (id) on delete set null;
