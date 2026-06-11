import { useEffect, useState } from "react";
import { api, ApiError, logout } from "../api";
import type { Achievement, Me, TopicProgress } from "../types";

export function SettingsView() {
  const [me, setMe] = useState<Me | null>(null);
  const [achievements, setAchievements] = useState<Achievement[]>([]);
  const [topics, setTopics] = useState<TopicProgress[]>([]);
  const [timezone, setTimezone] = useState("");
  const [dailyTaskGoal, setDailyTaskGoal] = useState(1);
  const [dailyCapacityMinutes, setDailyCapacityMinutes] = useState(120);
  const [status, setStatus] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api.me().then((m) => {
      setMe(m);
      setTimezone(m.timezone);
      setDailyTaskGoal(m.dailyTaskGoal);
      setDailyCapacityMinutes(m.dailyCapacityMinutes);
    }).catch((e: unknown) => setError(e instanceof ApiError ? e.message : String(e)));
    api.achievements().then(setAchievements).catch(() => setAchievements([]));
    api.topics().then(setTopics).catch(() => setTopics([]));
  }, []);

  const save = async () => {
    setStatus(null);
    setError(null);
    try {
      const updated = await api.updateSettings({ timezone, dailyTaskGoal, dailyCapacityMinutes });
      setMe(updated);
      setStatus("Saved.");
    } catch (e) {
      setError(e instanceof ApiError ? e.message : String(e));
    }
  };

  if (!me) return <div className="panel muted">Loading…</div>;

  return (
    <div className="panel">
      <h2>Settings</h2>
      <p className="muted">{me.email}</p>

      <div className="field">
        <label>Timezone (IANA) — used for streaks</label>
        <input value={timezone} onChange={(e) => setTimezone(e.target.value)}
          placeholder="Europe/Kyiv" />
      </div>
      <div className="field">
        <label>Daily task goal (tasks/day that count the streak)</label>
        <input type="number" min={1} value={dailyTaskGoal}
          onChange={(e) => setDailyTaskGoal(Number(e.target.value))} />
      </div>
      <div className="field">
        <label>Daily capacity (minutes)</label>
        <input type="number" min={1} value={dailyCapacityMinutes}
          onChange={(e) => setDailyCapacityMinutes(Number(e.target.value))} />
      </div>

      <div className="row">
        <button className="primary" onClick={save}>Save</button>
        <button onClick={logout}>Sign out</button>
      </div>
      {status && <p className="muted">{status}</p>}
      {error && <p className="error">{error}</p>}

      <h3 style={{ marginTop: "1.5rem" }}>Topics</h3>
      {topics.length === 0
        ? <p className="muted">No topics yet — they appear as you tag tasks.</p>
        : topics.map((t) => (
          <div className="task" key={t.slug}>
            <span>{t.name}</span>
            <span className="spacer" />
            <span className="muted" style={{ fontSize: "0.82rem" }}>
              {t.done}/{t.total} ({t.total > 0 ? Math.round((t.done / t.total) * 100) : 0}%)
            </span>
          </div>
        ))}

      <h3 style={{ marginTop: "1.5rem" }}>Achievements</h3>
      {achievements.length === 0
        ? <p className="muted">None unlocked yet — complete tasks and build a streak.</p>
        : achievements.map((a) => (
          <div className="task" key={a.code}>
            <span><strong>{a.title}</strong>{a.description ? ` — ${a.description}` : ""}</span>
            <span className="spacer" />
            <span className="muted" style={{ fontSize: "0.78rem" }}>{a.unlockedAt.slice(0, 10)}</span>
          </div>
        ))}
    </div>
  );
}
