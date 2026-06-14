import { useEffect, useState } from "react";
import { api, ApiError, logout } from "../api";
import type { AiSettings, Me, Plan } from "../types";

export function SettingsView() {
  const [me, setMe] = useState<Me | null>(null);
  const [plan, setPlan] = useState<Plan | null>(null);
  const [timezone, setTimezone] = useState("");
  const [dailyTaskGoal, setDailyTaskGoal] = useState(1);
  const [dailyCapacityMinutes, setDailyCapacityMinutes] = useState(120);
  const [status, setStatus] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  // BYOK AI provider.
  const [ai, setAi] = useState<AiSettings | null>(null);
  const [baseUrl, setBaseUrl] = useState("");
  const [model, setModel] = useState("");
  const [apiKey, setApiKey] = useState("");
  const [aiStatus, setAiStatus] = useState<string | null>(null);
  const [aiError, setAiError] = useState<string | null>(null);

  useEffect(() => {
    api.me().then((m) => {
      setMe(m);
      setTimezone(m.timezone);
      setDailyTaskGoal(m.dailyTaskGoal);
      setDailyCapacityMinutes(m.dailyCapacityMinutes);
    }).catch((e: unknown) => setError(e instanceof ApiError ? e.message : String(e)));
    loadAi();
    api.plan().then(setPlan).catch(() => setPlan(null));
  }, []);

  const changePlan = async (to: "pro" | "free") => {
    setError(null);
    try {
      setPlan(to === "pro" ? await api.upgradePlan() : await api.downgradePlan());
    } catch (e) {
      setError(e instanceof ApiError ? e.message : String(e));
    }
  };

  const loadAi = () => {
    api.aiSettings().then((s) => {
      setAi(s);
      setBaseUrl(s.baseUrl ?? "");
      setModel(s.model ?? "");
    }).catch(() => setAi(null));
  };

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

  const saveAi = async () => {
    setAiStatus(null);
    setAiError(null);
    try {
      const updated = await api.updateAiSettings({ baseUrl, model, apiKey: apiKey || undefined });
      setAi(updated);
      setApiKey("");
      setAiStatus("Saved. Your AI requests now use this provider.");
    } catch (e) {
      setAiError(e instanceof ApiError ? e.message : String(e));
    }
  };

  const clearAi = async () => {
    setAiStatus(null);
    setAiError(null);
    try {
      await api.clearAiSettings();
      setBaseUrl("");
      setModel("");
      setApiKey("");
      loadAi();
      setAiStatus("Cleared. Using the app default provider.");
    } catch (e) {
      setAiError(e instanceof ApiError ? e.message : String(e));
    }
  };

  const exportData = async () => {
    try {
      const data = await api.exportData();
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: "application/json" });
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "questline-export.json";
      a.click();
      URL.revokeObjectURL(url);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : String(e));
    }
  };

  const deleteAccount = async () => {
    if (!window.confirm("Delete your account and all your data? This cannot be undone.")) return;
    try {
      await api.deleteAccount();
      logout();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : String(e));
    }
  };

  if (!me) return <div className="panel muted">Loading…</div>;

  return (
    <div>
      <div className="panel">
        <h2 style={{ marginTop: 0 }}>Settings</h2>
        <p className="muted">{me.email}</p>

        <div className="field">
          <label>Timezone (IANA) — used for streaks</label>
          <input value={timezone} onChange={(e) => setTimezone(e.target.value)} placeholder="Europe/Kyiv" />
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

      {plan && (
        <div className="panel">
          <h3 style={{ marginTop: 0 }}>Plan</h3>
          <p className="muted">
            You're on <strong>{plan.plan}</strong> — up to <strong>{plan.aiDailyLimit}</strong> AI
            requests per day.
          </p>
          <div className="row">
            {plan.plan === "FREE"
              ? <button className="primary" onClick={() => changePlan("pro")}>Upgrade to PRO</button>
              : <button onClick={() => changePlan("free")}>Downgrade to FREE</button>}
          </div>
          <p className="muted" style={{ marginTop: "0.4rem", fontSize: "0.8rem" }}>
            Billing is a stub here (no real payment); a Stripe integration would replace it.
          </p>
        </div>
      )}

      <div className="panel">
        <h3 style={{ marginTop: 0 }}>AI provider (bring your own key)</h3>
        <p className="muted">
          Use your own OpenAI-compatible provider — OpenRouter, OpenAI, Groq, or a local model
          (Ollama, LM Studio). Leave empty to use the app default.{" "}
          <strong>{ai?.configured ? `Active: ${ai.model}` : "Currently: app default"}</strong>
        </p>
        <div className="field">
          <label>Base URL</label>
          <input value={baseUrl} onChange={(e) => setBaseUrl(e.target.value)}
            placeholder="https://openrouter.ai/api/v1" />
        </div>
        <div className="field">
          <label>Model</label>
          <input value={model} onChange={(e) => setModel(e.target.value)}
            placeholder="openai/gpt-4o-mini" />
        </div>
        <div className="field">
          <label>API key {ai?.configured && <span className="muted">(stored — leave blank to keep)</span>}</label>
          <input type="password" value={apiKey} onChange={(e) => setApiKey(e.target.value)}
            placeholder="sk-or-…" />
        </div>
        <div className="row">
          <button className="primary" disabled={!baseUrl.trim() || !model.trim()} onClick={saveAi}>Save</button>
          {ai?.configured && <button onClick={clearAi}>Use app default</button>}
        </div>
        {aiStatus && <p className="muted">{aiStatus}</p>}
        {aiError && <p className="error">{aiError}</p>}
      </div>

      <div className="panel">
        <h3 style={{ marginTop: 0 }}>Data &amp; account</h3>
        <div className="row">
          <button onClick={exportData}>Export my data</button>
          <button onClick={deleteAccount} style={{ borderColor: "var(--bad)", color: "var(--bad)" }}>
            Delete account
          </button>
        </div>
        <p className="muted" style={{ marginTop: "0.5rem", fontSize: "0.82rem" }}>
          Export downloads all your goals, tasks, streak and stats as JSON. Deleting removes your
          account and all data permanently.
        </p>
      </div>
    </div>
  );
}
