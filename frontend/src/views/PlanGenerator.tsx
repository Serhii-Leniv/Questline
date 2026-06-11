import { useState } from "react";
import { api, ApiError } from "../api";
import { PlanJobReview } from "./PlanJobReview";

export function PlanGenerator({ onAccepted }: { onAccepted: (goalId: string) => void }) {
  const [context, setContext] = useState("");
  const [target, setTarget] = useState("");
  const [weekly, setWeekly] = useState<number | "">("");
  const [job, setJob] = useState<{ jobId: string; goalId: string } | null>(null);
  const [error, setError] = useState<string | null>(null);

  const start = async () => {
    setError(null);
    try {
      setJob(await api.startPlan({
        context,
        target,
        weeklyCapacityMinutes: weekly === "" ? undefined : weekly,
      }));
    } catch (e) {
      setError(e instanceof ApiError ? e.message : String(e));
    }
  };

  if (job) {
    return <PlanJobReview jobId={job.jobId} goalId={job.goalId} onAccepted={onAccepted}
      workingLabel="Generating your roadmap…" />;
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
      {error && <p className="error">{error}</p>}
    </div>
  );
}
