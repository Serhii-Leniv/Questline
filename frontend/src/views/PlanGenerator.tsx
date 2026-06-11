import { useEffect, useState } from "react";
import { api, ApiError } from "../api";
import type { GeneratedPlan } from "../types";

type Phase = "form" | "generating" | "ready" | "failed";

export function PlanGenerator({ onAccepted }: { onAccepted: (goalId: string) => void }) {
  const [phase, setPhase] = useState<Phase>("form");
  const [context, setContext] = useState("");
  const [target, setTarget] = useState("");
  const [weekly, setWeekly] = useState<number | "">("");
  const [jobId, setJobId] = useState<string | null>(null);
  const [goalId, setGoalId] = useState<string | null>(null);
  const [plan, setPlan] = useState<GeneratedPlan | null>(null);
  const [error, setError] = useState<string | null>(null);

  const start = async () => {
    setError(null);
    try {
      const res = await api.startPlan({
        context,
        target,
        weeklyCapacityMinutes: weekly === "" ? undefined : weekly,
      });
      setJobId(res.jobId);
      setGoalId(res.goalId);
      setPhase("generating");
    } catch (e) {
      setError(e instanceof ApiError ? e.message : String(e));
    }
  };

  // Poll the job while it generates.
  useEffect(() => {
    if (phase !== "generating" || !jobId) return;
    const tick = async () => {
      try {
        const job = await api.getJob(jobId);
        if (job.status === "SUCCEEDED") {
          setPlan(job.plan);
          setPhase("ready");
        } else if (job.status === "FAILED") {
          setError(job.error ?? "Generation failed");
          setPhase("failed");
        }
      } catch (e) {
        setError(e instanceof ApiError ? e.message : String(e));
        setPhase("failed");
      }
    };
    const id = setInterval(tick, 1500);
    void tick();
    return () => clearInterval(id);
  }, [phase, jobId]);

  const accept = async () => {
    if (!goalId) return;
    try {
      await api.acceptPlan(goalId);
      onAccepted(goalId);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : String(e));
    }
  };

  if (phase === "generating") {
    return <div className="panel"><h3>Generating your roadmap…</h3>
      <p className="muted">The AI is decomposing your goal. This runs in the background.</p></div>;
  }

  if (phase === "ready" && plan) {
    return (
      <div className="panel">
        <h3>Proposed roadmap</h3>
        <p className="muted">{plan.summary}</p>
        {plan.milestones.map((m, i) => (
          <div key={i} style={{ marginBottom: "0.5rem" }}>
            <strong>{m.title}</strong>
            <ul style={{ margin: "0.25rem 0" }}>
              {m.tasks.map((t, j) => (
                <li key={j}>{t.title}{t.estimateMinutes != null ? ` (${t.estimateMinutes}m)` : ""}</li>
              ))}
            </ul>
          </div>
        ))}
        <div className="row">
          <button className="primary" onClick={accept}>Accept &amp; save</button>
        </div>
        {error && <p className="error">{error}</p>}
      </div>
    );
  }

  return (
    <div className="panel">
      <h3>Generate a plan with AI</h3>
      <div className="field">
        <label>Where you are now (context)</label>
        <textarea rows={2} value={context} onChange={(e) => setContext(e.target.value)}
          placeholder="Mid-level backend dev, comfortable with Java" />
      </div>
      <div className="field">
        <label>What you want to become (target)</label>
        <textarea rows={2} value={target} onChange={(e) => setTarget(e.target.value)}
          placeholder="Senior engineer strong in system design & concurrency" />
      </div>
      <div className="field">
        <label>Weekly capacity (minutes, optional)</label>
        <input type="number" min={1} value={weekly}
          onChange={(e) => setWeekly(e.target.value === "" ? "" : Number(e.target.value))} />
      </div>
      <button className="primary" disabled={!context.trim() || !target.trim()} onClick={start}>
        Generate
      </button>
      {phase === "failed" && error && (
        <p className="error">{error} <button onClick={() => setPhase("form")}>Try again</button></p>
      )}
    </div>
  );
}
