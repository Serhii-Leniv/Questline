-- Achievements unlocked by goal-progress tracking (Phase 2).

insert into achievements (id, code, title, description) values
    (gen_random_uuid(), 'MILESTONE_DONE', 'Milestone cleared', 'Finish every task in a milestone'),
    (gen_random_uuid(), 'GOAL_DONE',      'Goal complete',     'Complete an entire goal');
