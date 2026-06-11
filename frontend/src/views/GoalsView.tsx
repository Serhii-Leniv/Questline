import { useEffect, useState } from "react";
import { api, ApiError } from "../api";
import type { Goal } from "../types";
import { GoalDetail } from "./GoalDetail";
import { PlanGenerator } from "./PlanGenerator";
import { RoadmapImporter } from "./RoadmapImporter";

type Mode = "ai" | "import";

export function GoalsView({ onChanged }: { onChanged: () => void }) {
  const [goals, setGoals] = useState<Goal[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<string | null>(null);
  const [mode, setMode] = useState<Mode | null>(null);

  const load = () => {
    setError(null);
    api.listGoals("ACTIVE").then(setGoals).catch((e: unknown) =>
      setError(e instanceof ApiError ? e.message : String(e)),
    );
  };

  useEffect(load, []);

  if (selected) {
    return (
      <GoalDetail
        goalId={selected}
        onBack={() => { setSelected(null); load(); }}
        onChanged={onChanged}
      />
    );
  }

  if (mode) {
    const onAccepted = (goalId: string) => { setMode(null); setSelected(goalId); };
    return (
      <div>
        <button onClick={() => setMode(null)} style={{ marginBottom: "0.75rem" }}>← Goals</button>
        {mode === "ai" ? <PlanGenerator onAccepted={onAccepted} />
          : <RoadmapImporter onAccepted={onAccepted} />}
      </div>
    );
  }

  return (
    <div>
      <div className="row" style={{ marginBottom: "1rem" }}>
        <h2 style={{ margin: 0 }}>Your goals</h2>
        <span style={{ display: "flex", gap: "0.5rem" }}>
          <button onClick={() => setMode("import")}>Import roadmap</button>
          <button className="primary" onClick={() => setMode("ai")}>New goal with AI</button>
        </span>
      </div>

      {error && <div className="panel error">{error}</div>}
      {!goals && <div className="panel muted">Loading…</div>}
      {goals && goals.length === 0 && (
        <div className="panel muted">No active goals yet. Generate one with AI to get started.</div>
      )}
      {goals?.map((goal) => (
        <div className="panel" key={goal.id}>
          <div className="row">
            <button onClick={() => setSelected(goal.id)} style={{ textAlign: "left" }}>
              <strong>{goal.title}</strong>
            </button>
            <span className="spacer" />
            <span className="tag">{Math.round(goal.progress * 100)}%</span>
          </div>
        </div>
      ))}
    </div>
  );
}
