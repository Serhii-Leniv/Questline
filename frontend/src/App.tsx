import { useCallback, useEffect, useState } from "react";
import { api, getToken, setToken } from "./api";
import type { Overview } from "./types";
import { Login } from "./views/Login";
import { TodayView } from "./views/TodayView";
import { WeekView } from "./views/WeekView";
import { GoalsView } from "./views/GoalsView";
import { StatsView } from "./views/StatsView";
import { SettingsView } from "./views/SettingsView";

type Tab = "today" | "week" | "goals" | "stats" | "settings";

/** On the OAuth callback route, pull the JWT out of the URL fragment and store it. */
function captureCallbackToken(): void {
  if (window.location.pathname !== "/login/callback") return;
  const hash = window.location.hash.replace(/^#/, "");
  const token = new URLSearchParams(hash).get("token");
  if (token) setToken(token);
  window.history.replaceState({}, "", "/");
}

export function App() {
  const [authed] = useState<boolean>(() => {
    captureCallbackToken();
    return getToken() != null;
  });
  const [tab, setTab] = useState<Tab>("today");
  const [overview, setOverview] = useState<Overview | null>(null);

  const refreshStats = useCallback(() => {
    api.overview().then(setOverview).catch(() => setOverview(null));
  }, []);

  useEffect(() => {
    if (authed) refreshStats();
  }, [authed, refreshStats]);

  if (!authed) return <Login />;

  return (
    <div className="app">
      <header className="topbar">
        <span className="brand">Questline</span>
        {overview && (
          <div className="stats">
            <span>Streak <b>{overview.currentStreak}</b></span>
            {overview.freezesAvailable > 0 && <span>Freezes <b>{overview.freezesAvailable}</b></span>}
            <span>Level <b>{overview.level}</b></span>
            <span><b>{overview.xpTotal}</b> XP</span>
          </div>
        )}
      </header>

      <nav className="tabs">
        <button aria-current={tab === "today"} onClick={() => setTab("today")}>Today</button>
        <button aria-current={tab === "week"} onClick={() => setTab("week")}>Week</button>
        <button aria-current={tab === "goals"} onClick={() => setTab("goals")}>Goals</button>
        <button aria-current={tab === "stats"} onClick={() => setTab("stats")}>Stats</button>
        <button aria-current={tab === "settings"} onClick={() => setTab("settings")}>Settings</button>
      </nav>

      {tab === "today" && <TodayView onChanged={refreshStats} />}
      {tab === "week" && <WeekView onChanged={refreshStats} />}
      {tab === "goals" && <GoalsView onChanged={refreshStats} />}
      {tab === "stats" && <StatsView />}
      {tab === "settings" && <SettingsView />}
    </div>
  );
}
