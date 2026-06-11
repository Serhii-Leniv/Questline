import { useEffect, useState } from "react";
import { api, ApiError } from "../api";
import type { TemplateSummary } from "../types";

export function TemplatesBrowser({ onImported }: { onImported: (goalId: string) => void }) {
  const [templates, setTemplates] = useState<TemplateSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState<string | null>(null);

  useEffect(() => {
    api.templates().then(setTemplates)
      .catch((e: unknown) => setError(e instanceof ApiError ? e.message : String(e)));
  }, []);

  const importIt = async (id: string) => {
    setBusy(id);
    setError(null);
    try {
      const goal = await api.importTemplate(id);
      onImported(goal.id);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : String(e));
    } finally {
      setBusy(null);
    }
  };

  if (error) return <div className="panel error">{error}</div>;
  if (!templates) return <div className="panel muted">Loading…</div>;
  if (templates.length === 0) {
    return <div className="panel muted">No templates yet — publish a goal to share the first one.</div>;
  }

  return (
    <div>
      {templates.map((t) => (
        <div className="panel" key={t.id}>
          <div className="row">
            <strong>{t.title}</strong>
            <span className="spacer" />
            <span className="tag">{t.taskCount} tasks</span>
            <button disabled={busy === t.id} onClick={() => importIt(t.id)}>
              {busy === t.id ? "Importing…" : "Import"}
            </button>
          </div>
          {t.summary && <p className="muted" style={{ marginTop: "0.4rem" }}>{t.summary}</p>}
        </div>
      ))}
    </div>
  );
}
