import { useEffect, useState } from "react";
import { api, ApiError } from "../api";
import type { Task } from "../types";

export function TodayView({ onChanged }: { onChanged: () => void }) {
  const [tasks, setTasks] = useState<Task[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState<string | null>(null);

  const load = () => {
    setError(null);
    api.today().then(setTasks).catch((e: unknown) =>
      setError(e instanceof ApiError ? e.message : String(e)),
    );
  };

  useEffect(load, []);

  const complete = async (task: Task) => {
    setBusy(task.id);
    try {
      const updated = await api.setTaskStatus(task.id, "DONE");
      setTasks((cur) => cur?.map((t) => (t.id === task.id ? updated : t)) ?? null);
      onChanged();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : String(e));
    } finally {
      setBusy(null);
    }
  };

  const planDay = async () => {
    setBusy("plan");
    setError(null);
    try {
      setTasks(await api.planDay());
    } catch (e) {
      setError(e instanceof ApiError ? e.message : String(e));
    } finally {
      setBusy(null);
    }
  };

  if (error) return <div className="panel error">{error}</div>;
  if (!tasks) return <div className="panel muted">Loading…</div>;

  return (
    <div className="panel">
      <div className="row">
        <h2 style={{ margin: 0 }}>Today</h2>
        <button onClick={planDay} disabled={busy === "plan"}>Plan my day</button>
      </div>
      {tasks.length === 0 && (
        <p className="muted">
          Nothing scheduled for today. Use “Plan my day”, or open a goal and schedule a task.
        </p>
      )}
      {tasks.map((task) => {
        const done = task.status === "DONE";
        return (
          <div className="task" key={task.id}>
            <input type="checkbox" checked={done} disabled={done || busy === task.id}
              onChange={() => complete(task)} />
            <span className={done ? "done" : ""}>{task.title}</span>
            {task.estimateMinutes != null && <span className="tag">{task.estimateMinutes}m</span>}
            {task.topics.map((tp) => <span className="tag" key={tp}>{tp}</span>)}
            <span className="spacer" />
            {done && <span className="tag">done</span>}
          </div>
        );
      })}
    </div>
  );
}
