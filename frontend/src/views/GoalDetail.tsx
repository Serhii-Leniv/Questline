import { useCallback, useEffect, useState } from "react";
import { api, ApiError } from "../api";
import type { GoalTree, TaskNode } from "../types";
import { PlanJobReview } from "./PlanJobReview";

/** Browser-local today as YYYY-MM-DD (used to schedule a task onto the Today view). */
function localToday(): string {
  const d = new Date();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${d.getFullYear()}-${m}-${day}`;
}

export function GoalDetail(
  { goalId, onBack, onChanged }: { goalId: string; onBack: () => void; onChanged: () => void },
) {
  const [tree, setTree] = useState<GoalTree | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [decomposing, setDecomposing] = useState<string | null>(null);
  const [replanJob, setReplanJob] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const load = useCallback(() => {
    setError(null);
    api.getGoal(goalId).then(setTree).catch((e: unknown) =>
      setError(e instanceof ApiError ? e.message : String(e)),
    );
  }, [goalId]);

  useEffect(load, [load]);

  const complete = async (task: TaskNode) => {
    try {
      await api.setTaskStatus(task.id, "DONE");
      onChanged();
      load();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : String(e));
    }
  };

  const scheduleToday = async (task: TaskNode) => {
    try {
      await api.scheduleTask(task.id, localToday());
      load();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : String(e));
    }
  };

  const breakDown = async (taskId: string) => {
    setDecomposing(taskId);
    setError(null);
    try {
      const { jobId } = await api.decomposeTask(taskId);
      await pollJob(jobId);
      load();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : String(e));
    } finally {
      setDecomposing(null);
    }
  };

  const pollJob = (jobId: string) =>
    new Promise<void>((resolve) => {
      const id = setInterval(async () => {
        try {
          const job = await api.getJob(jobId);
          if (job.status === "SUCCEEDED" || job.status === "FAILED") {
            clearInterval(id);
            if (job.status === "FAILED") setError(job.error ?? "Could not break down the task");
            resolve();
          }
        } catch (e) {
          clearInterval(id);
          setError(e instanceof ApiError ? e.message : String(e));
          resolve();
        }
      }, 1500);
    });

  const replan = async () => {
    setError(null);
    try {
      const { jobId } = await api.replanGoal(goalId);
      setReplanJob(jobId);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : String(e));
    }
  };

  const publish = async () => {
    setError(null);
    setNotice(null);
    try {
      await api.publishGoal(goalId);
      setNotice("Published as a public template.");
    } catch (e) {
      setError(e instanceof ApiError ? e.message : String(e));
    }
  };

  const renderTask = (task: TaskNode, depth: number) => {
    const done = task.status === "DONE";
    return (
      <div key={task.id}>
        <div className="task" style={{ paddingLeft: depth * 18 }}>
          <input type="checkbox" checked={done} disabled={done} onChange={() => complete(task)} />
          <span className={done ? "done" : ""}>{task.title}</span>
          {task.estimateMinutes != null && <span className="tag">{task.estimateMinutes}m</span>}
          {task.topics.map((tp) => <span className="tag" key={tp}>{tp}</span>)}
          <span className="spacer" />
          {task.scheduledFor && <span className="tag">{task.scheduledFor}</span>}
          {!done && !task.scheduledFor && (
            <button onClick={() => scheduleToday(task)}>Schedule today</button>
          )}
          {depth === 0 && (
            <button disabled={decomposing === task.id} onClick={() => breakDown(task.id)}>
              {decomposing === task.id ? "Breaking down…" : "Break down"}
            </button>
          )}
        </div>
        {task.subtasks.map((s) => renderTask(s, depth + 1))}
      </div>
    );
  };

  if (error && !tree) {
    return <div className="panel error">{error} <button onClick={onBack}>Back</button></div>;
  }
  if (!tree) return <div className="panel muted">Loading…</div>;

  if (replanJob) {
    return (
      <div>
        <button onClick={() => setReplanJob(null)} style={{ marginBottom: "0.75rem" }}>← Back</button>
        <PlanJobReview
          jobId={replanJob}
          goalId={goalId}
          workingLabel="Replanning the remaining work…"
          accept={(id) => api.acceptReplan(id)}
          onAccepted={() => { setReplanJob(null); onChanged(); load(); }}
        />
      </div>
    );
  }

  return (
    <div>
      <div className="row" style={{ marginBottom: "0.75rem" }}>
        <button onClick={onBack}>← Goals</button>
        <span className="spacer" />
        <span className="tag">{Math.round(tree.progress * 100)}% done</span>
        <button onClick={publish}>Publish</button>
        <button onClick={replan}>Replan</button>
      </div>
      {notice && <p className="muted">{notice}</p>}
      <div className="panel">
        <h2>{tree.title}</h2>
        {tree.target && <p className="muted">{tree.target}</p>}
      </div>

      {error && <div className="panel error">{error}</div>}
      {tree.milestones.length === 0 && <div className="panel muted">No milestones yet.</div>}
      {tree.milestones.map((m) => (
        <div className="panel" key={m.id}>
          <strong>{m.title}</strong>
          {m.description && <p className="muted" style={{ marginTop: "0.25rem" }}>{m.description}</p>}
          {m.tasks.map((t) => renderTask(t, 0))}
        </div>
      ))}
    </div>
  );
}
