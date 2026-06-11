import { useEffect, useState } from "react";
import { api, ApiError, logout } from "../api";
import type { Me } from "../types";

export function SettingsView() {
  const [me, setMe] = useState<Me | null>(null);
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
    </div>
  );
}
