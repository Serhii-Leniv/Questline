import { useEffect, useState } from "react";
import { api, ApiError, logout } from "../api";
import type { AiSettings, Me } from "../types";

export function SettingsView() {
  const [me, setMe] = useState<Me | null>(null);
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
  }, []);

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
    </div>
  );
}
