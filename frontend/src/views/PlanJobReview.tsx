import { useEffect, useState } from "react";
import { api, ApiError } from "../api";
import type { GeneratedPlan } from "../types";

/**
 * Polls an AI job to completion, shows the resulting plan, lets the user refine it in chat
 * (re-polling the new job), and accepts it into the goal.
 */
export function PlanJobReview(
  { jobId, goalId, onAccepted, workingLabel, accept = (id) => api.acceptPlan(id) }:
  {
    jobId: string;
    goalId: string;
    onAccepted: (goalId: string) => void;
    workingLabel: string;
    accept?: (goalId: string) => Promise<unknown>;
  },
) {
  const [currentJob, setCurrentJob] = useState(jobId);
  const [plan, setPlan] = useState<GeneratedPlan | null>(null);
  const [phase, setPhase] = useState<"working" | "ready" | "failed">("working");
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState("");
  const [refining, setRefining] = useState(false);

  useEffect(() => {
    if (phase !== "working") return;
    const tick = async () => {
      try {
        const job = await api.getJob(currentJob);
        if (job.status === "SUCCEEDED") {
          setPlan(job.plan);
          setPhase("ready");
        } else if (job.status === "FAILED") {
          setError(job.error ?? "The job failed");
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
  }, [phase, currentJob]);

  const doAccept = async () => {
    try {
      await accept(goalId);
      onAccepted(goalId);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : String(e));
    }
  };

  const refine = async () => {
    if (!message.trim()) return;
    setRefining(true);
    setError(null);
    try {
      const { jobId: newJob } = await api.refinePlan(goalId, message.trim());
      setMessage("");
      setCurrentJob(newJob);
      setPhase("working");
    } catch (e) {
      setError(e instanceof ApiError ? e.message : String(e));
    } finally {
      setRefining(false);
    }
  };

  if (phase === "working") {
    return <div className="panel"><h3>{workingLabel}</h3>
      <p className="muted">This runs in the background.</p></div>;
  }
  if (phase === "failed") {
    return <div className="panel"><p className="error">{error}</p></div>;
  }
  if (!plan) return null;

  return (
    <div className="panel">
      <h3>Proposed roadmap</h3>
      {plan.summary && <p className="muted">{plan.summary}</p>}
      {plan.milestones.map((m, i) => (
        <div key={i} style={{ marginBottom: "0.6rem" }}>
          <strong>{m.title}</strong>
          <ul style={{ margin: "0.25rem 0" }}>
            {m.tasks.map((t, j) => (
              <li key={j}>{t.title}{t.estimateMinutes != null ? ` (${t.estimateMinutes}m)` : ""}</li>
            ))}
          </ul>
        </div>
      ))}

      <div className="field" style={{ marginTop: "0.75rem" }}>
        <label>Refine — ask for changes (e.g. “make it 3 months”, “I already know SQL”)</label>
        <textarea rows={2} value={message} onChange={(e) => setMessage(e.target.value)} />
      </div>
      <div className="row">
        <button onClick={refine} disabled={refining || !message.trim()}>
          {refining ? "Refining…" : "Refine"}
        </button>
        <button className="primary" onClick={doAccept}>Accept &amp; save</button>
      </div>
      {error && <p className="error">{error}</p>}
    </div>
  );
}
