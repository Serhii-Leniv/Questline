import { useEffect, useState } from "react";
import { api, ApiError } from "../api";
import type { Task } from "../types";

function isoDate(d: Date): string {
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${d.getFullYear()}-${m}-${day}`;
}

/** Monday of the current local week. */
function weekStart(): Date {
  const d = new Date();
  const offset = (d.getDay() + 6) % 7; // Mon = 0 … Sun = 6
  d.setDate(d.getDate() - offset);
  d.setHours(0, 0, 0, 0);
  return d;
}

const DAY_NAMES = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];

export function WeekView({ onChanged }: { onChanged: () => void }) {
  const [tasks, setTasks] = useState<Task[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = () => {
    setError(null);
    api.week().then(setTasks).catch((e: unknown) =>
      setError(e instanceof ApiError ? e.message : String(e)),
    );
  };

  useEffect(load, []);

  const complete = async (task: Task) => {
    try {
      const updated = await api.setTaskStatus(task.id, "DONE");
      setTasks((cur) => cur?.map((t) => (t.id === task.id ? updated : t)) ?? null);
      onChanged();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : String(e));
    }
  };

  if (error) return <div className="panel error">{error}</div>;
  if (!tasks) return <div className="panel muted">Loading…</div>;

  const start = weekStart();
  const today = isoDate(new Date());

  return (
    <div>
      <h2 style={{ marginTop: 0 }}>This week</h2>
      {DAY_NAMES.map((name, i) => {
        const date = new Date(start);
        date.setDate(start.getDate() + i);
        const key = isoDate(date);
        const dayTasks = tasks.filter((t) => t.scheduledFor === key);
        return (
          <div className="panel" key={key} style={{ outline: key === today ? "1px solid var(--accent)" : "none" }}>
            <div className="row">
              <strong>{name}</strong>
              <span className="muted" style={{ fontSize: "0.8rem" }}>{key}</span>
            </div>
            {dayTasks.length === 0
              ? <p className="muted" style={{ margin: "0.4rem 0 0" }}>—</p>
              : dayTasks.map((t) => {
                const done = t.status === "DONE";
                return (
                  <div className="task" key={t.id}>
                    <input type="checkbox" checked={done} disabled={done}
                      onChange={() => complete(t)} />
                    <span className={done ? "done" : ""}>{t.title}</span>
                    {t.estimateMinutes != null && <span className="tag">{t.estimateMinutes}m</span>}
                  </div>
                );
              })}
          </div>
        );
      })}
    </div>
  );
}
