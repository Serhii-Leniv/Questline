import { useCallback, useEffect, useState } from "react";
import { api, ApiError } from "../api";
import type { GoalTree, TaskNode } from "../types";

/** Browser-local today as YYYY-MM-DD (used to schedule a task onto the Today view). */
function localToday(): string {
  const d = new Date();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${d.getFullYear()}-${m}-${day}`;
}

export function GoalDetail(
  { goalId, onBack, onChanged }: { goalId: string; onBack: () => void; onChanged: () => void },
) {
  const [tree, setTree] = useState<GoalTree | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    setError(null);
    api.getGoal(goalId).then(setTree).catch((e: unknown) =>
      setError(e instanceof ApiError ? e.message : String(e)),
    );
  }, [goalId]);

  useEffect(load, [load]);

  const complete = async (task: TaskNode) => {
    try {
      await api.setTaskStatus(task.id, "DONE");
      onChanged();
      load();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : String(e));
    }
  };

  const scheduleToday = async (task: TaskNode) => {
    try {
      await api.scheduleTask(task.id, localToday());
      load();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : String(e));
    }
  };

  if (error) return <div className="panel error">{error} <button onClick={onBack}>Back</button></div>;
  if (!tree) return <div className="panel muted">Loading…</div>;

  return (
    <div>
      <div className="row" style={{ marginBottom: "0.75rem" }}>
        <button onClick={onBack}>← Goals</button>
        <span className="spacer" />
        <span className="tag">{Math.round(tree.progress * 100)}% done</span>
      </div>
      <div className="panel">
        <h2>{tree.title}</h2>
        {tree.target && <p className="muted">{tree.target}</p>}
      </div>

      {tree.milestones.length === 0 && (
        <div className="panel muted">No milestones yet.</div>
      )}
      {tree.milestones.map((m) => (
        <div className="panel" key={m.id}>
          <strong>{m.title}</strong>
          {m.description && <p className="muted" style={{ marginTop: "0.25rem" }}>{m.description}</p>}
          {m.tasks.map((t) => {
            const done = t.status === "DONE";
            return (
              <div className="task" key={t.id}>
                <input type="checkbox" checked={done} disabled={done} onChange={() => complete(t)} />
                <span className={done ? "done" : ""}>{t.title}</span>
                {t.estimateMinutes != null && <span className="tag">{t.estimateMinutes}m</span>}
                <span className="spacer" />
                {t.scheduledFor
                  ? <span className="tag">{t.scheduledFor}</span>
                  : !done && <button onClick={() => scheduleToday(t)}>Schedule today</button>}
              </div>
            );
          })}
        </div>
      ))}
    </div>
  );
}
