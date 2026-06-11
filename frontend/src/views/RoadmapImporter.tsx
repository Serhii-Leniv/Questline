import { useState } from "react";
import { api, ApiError } from "../api";
import { PlanJobReview } from "./PlanJobReview";

export function RoadmapImporter({ onAccepted }: { onAccepted: (goalId: string) => void }) {
  const [text, setText] = useState("");
  const [job, setJob] = useState<{ jobId: string; goalId: string } | null>(null);
  const [error, setError] = useState<string | null>(null);

  const start = async () => {
    setError(null);
    try {
      setJob(await api.parseRoadmap(text));
    } catch (e) {
      setError(e instanceof ApiError ? e.message : String(e));
    }
  };

  if (job) {
    return <PlanJobReview jobId={job.jobId} goalId={job.goalId} onAccepted={onAccepted}
      workingLabel="Parsing your roadmap…" />;
  }

  return (
    <div className="panel">
      <h3>Import a roadmap</h3>
      <div className="field">
        <label>Paste your roadmap (markdown or plain text)</label>
        <textarea rows={8} value={text} onChange={(e) => setText(e.target.value)}
          placeholder={"Phase 1: Foundations\n- Learn the basics\n- Practice daily\n\nPhase 2: Depth\n- Build a project"} />
      </div>
      <button className="primary" disabled={!text.trim()} onClick={start}>Parse</button>
      {error && <p className="error">{error}</p>}
    </div>
  );
}
