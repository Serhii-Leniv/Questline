import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// Dev server proxies backend routes to Spring Boot so the SPA can use relative URLs (no CORS).
// /api — REST. /oauth2 + /login/oauth2 — Google OAuth handshake. /login/callback stays on the
// SPA (that's where the backend redirects with the JWT), so it is NOT proxied.
const target = process.env.VITE_BACKEND_URL ?? "http://localhost:8080";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": { target, changeOrigin: true },
      "/oauth2": { target, changeOrigin: true },
      "/login/oauth2": { target, changeOrigin: true },
    },
  },
});
