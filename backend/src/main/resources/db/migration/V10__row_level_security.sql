-- Row-Level Security as a second line of tenant isolation (Phase 3). Beyond the app's per-userId
-- scoping, the DB itself filters rows by app.user_id (set per transaction by RlsTransactionManager).
--
-- The policy passes through when app.user_id is unset, so background jobs / service access that have
-- no user context still work. FORCE makes the policy apply even to the table owner (so it works when
-- the app connects as the owner — but NOT as a superuser, which always bypasses RLS; run the app as
-- a non-superuser role in production).

do $$
declare
    t text;
begin
    foreach t in array array['goals', 'tasks', 'activity_days', 'streaks', 'topics', 'ai_jobs', 'user_achievements']
    loop
        execute format('alter table %I enable row level security', t);
        execute format('alter table %I force row level security', t);
        -- nullif(..., '') treats both an unset GUC (NULL) and a reset GUC ('') as "no user context",
        -- and avoids casting an empty string to uuid.
        execute format(
            'create policy tenant_isolation on %I'
            || ' using (nullif(current_setting(''app.user_id'', true), '''') is null'
            || ' or user_id = nullif(current_setting(''app.user_id'', true), '''')::uuid)'
            || ' with check (nullif(current_setting(''app.user_id'', true), '''') is null'
            || ' or user_id = nullif(current_setting(''app.user_id'', true), '''')::uuid)', t);
    end loop;
end $$;
