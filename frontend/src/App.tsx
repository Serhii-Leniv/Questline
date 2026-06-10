import { useEffect, useState } from "react";

type PingState =
  | { kind: "loading" }
  | { kind: "ok"; status: string }
  | { kind: "error"; message: string };

/**
 * Phase 0 smoke page: calls the backend /api/ping (through the Vite dev proxy) and shows
 * the result. Proves the frontend dev server reaches the backend.
 */
export function App() {
  const [ping, setPing] = useState<PingState>({ kind: "loading" });

  useEffect(() => {
    fetch("/api/ping")
      .then((res) => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json() as Promise<{ status: string }>;
      })
      .then((body) => setPing({ kind: "ok", status: body.status }))
      .catch((err: unknown) =>
        setPing({ kind: "error", message: err instanceof Error ? err.message : String(err) }),
      );
  }, []);

  return (
    <main style={{ fontFamily: "system-ui, sans-serif", padding: "2rem", maxWidth: 480 }}>
      <h1>Questline</h1>
      <p>AI-powered goal decomposition &amp; streak tracker — Phase 0 skeleton.</p>
      <section>
        <h2>Backend status</h2>
        {ping.kind === "loading" && <p>Checking <code>/api/ping</code>…</p>}
        {ping.kind === "ok" && (
          <p style={{ color: "green" }}>
            Backend reachable: <strong>{ping.status}</strong>
          </p>
        )}
        {ping.kind === "error" && (
          <p style={{ color: "crimson" }}>
            Could not reach backend: {ping.message}
          </p>
        )}
      </section>
    </main>
  );
}
