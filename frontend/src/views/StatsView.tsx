import { useEffect, useState } from "react";
import { api, ApiError } from "../api";
import type { Achievement, HeatmapEntry, Overview, TopicProgress } from "../types";

const WEEKS = 18;

function isoDate(d: Date): string {
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${d.getFullYear()}-${m}-${day}`;
}

/** Intensity 0–4 from a day's completed-task count. */
function level(count: number): number {
  if (count <= 0) return 0;
  if (count === 1) return 1;
  if (count <= 3) return 2;
  if (count <= 5) return 3;
  return 4;
}

const CELL_BG = ["var(--line)", "rgba(47,72,88,0.35)", "rgba(47,72,88,0.55)", "rgba(47,72,88,0.78)", "var(--accent)"];

export function StatsView() {
  const [overview, setOverview] = useState<Overview | null>(null);
  const [counts, setCounts] = useState<Map<string, number>>(new Map());
  const [achievements, setAchievements] = useState<Achievement[]>([]);
  const [topics, setTopics] = useState<TopicProgress[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const today = new Date();
    const from = new Date(today);
    from.setDate(today.getDate() - (WEEKS * 7 - 1));
    api.heatmap(isoDate(from), isoDate(today))
      .then((entries: HeatmapEntry[]) => setCounts(new Map(entries.map((e) => [e.date, e.count]))))
      .catch((e: unknown) => setError(e instanceof ApiError ? e.message : String(e)));
    api.overview().then(setOverview).catch(() => setOverview(null));
    api.achievements().then(setAchievements).catch(() => setAchievements([]));
    api.topics().then(setTopics).catch(() => setTopics([]));
  }, []);

  // Grid columns = weeks, starting on the Monday WEEKS-1 weeks ago.
  const start = new Date();
  start.setDate(start.getDate() - ((start.getDay() + 6) % 7) - (WEEKS - 1) * 7);
  const todayIso = isoDate(new Date());

  return (
    <div>
      <h2 style={{ marginTop: 0 }}>Stats</h2>

      {overview && (
        <div className="panel">
          <div className="stats" style={{ fontSize: "0.95rem" }}>
            <span>Streak <b>{overview.currentStreak}</b></span>
            <span>Longest <b>{overview.longestStreak}</b></span>
            <span>Level <b>{overview.level}</b></span>
            <span><b>{overview.xpTotal}</b> XP</span>
            {overview.freezesAvailable > 0 && <span>Freezes <b>{overview.freezesAvailable}</b></span>}
          </div>
        </div>
      )}

      <div className="panel">
        <h3 style={{ marginTop: 0 }}>Activity</h3>
        {error && <p className="error">{error}</p>}
        <div style={{ display: "flex", gap: 3, overflowX: "auto" }}>
          {Array.from({ length: WEEKS }, (_, w) => (
            <div key={w} style={{ display: "flex", flexDirection: "column", gap: 3 }}>
              {Array.from({ length: 7 }, (_, d) => {
                const cell = new Date(start);
                cell.setDate(start.getDate() + w * 7 + d);
                const key = isoDate(cell);
                const count = counts.get(key) ?? 0;
                const future = key > todayIso;
                return (
                  <div key={d} title={`${key}: ${count}`} style={{
                    width: 12, height: 12, borderRadius: 2,
                    background: future ? "transparent" : CELL_BG[level(count)],
                  }} />
                );
              })}
            </div>
          ))}
        </div>
      </div>

      <div className="panel">
        <h3 style={{ marginTop: 0 }}>Topics</h3>
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
      </div>

      <div className="panel">
        <h3 style={{ marginTop: 0 }}>Achievements</h3>
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
    </div>
  );
}
