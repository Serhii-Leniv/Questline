import { login } from "../api";

export function Login() {
  return (
    <div className="app">
      <header className="topbar">
        <span className="brand">Questline</span>
      </header>
      <div className="panel">
        <h2>Sign in</h2>
        <p className="muted">
          AI-powered goal decomposition &amp; streak tracker. Sign in to formulate a goal, let the
          AI break it into a roadmap, and keep your daily streak.
        </p>
        <button className="primary" onClick={login}>Sign in with Google</button>
      </div>
    </div>
  );
}
