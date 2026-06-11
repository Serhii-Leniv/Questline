import type {
  Achievement,
  AiJob,
  AiSettings,
  Goal,
  GoalTree,
  HeatmapEntry,
  Me,
  Overview,
  PlanJob,
  Task,
  TopicProgress,
} from "./types";

const TOKEN_KEY = "questline_token";

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY);
}

/** Full-page redirect into the backend's Google OAuth flow. */
export function login(): void {
  window.location.href = "/oauth2/authorization/google";
}

export function logout(): void {
  clearToken();
  window.location.assign("/");
}

/** Error carrying the backend's { code, message } contract. */
export class ApiError extends Error {
  constructor(readonly code: string, message: string) {
    super(message);
  }
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = {};
  const token = getToken();
  if (token) headers["Authorization"] = `Bearer ${token}`;
  if (body !== undefined) headers["Content-Type"] = "application/json";

  const res = await fetch(`/api${path}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (res.status === 401) {
    clearToken();
    window.location.assign("/");
    throw new ApiError("UNAUTHORIZED", "Session expired");
  }
  if (res.status === 204) return undefined as T;

  const text = await res.text();
  const data: unknown = text ? JSON.parse(text) : undefined;
  if (!res.ok) {
    const err = data as { code?: string; message?: string } | undefined;
    throw new ApiError(err?.code ?? "ERROR", err?.message ?? `HTTP ${res.status}`);
  }
  return data as T;
}

export const api = {
  me: () => request<Me>("GET", "/me"),
  updateSettings: (body: { timezone?: string; dailyCapacityMinutes?: number; dailyTaskGoal?: number }) =>
    request<Me>("PATCH", "/me/settings", body),

  aiSettings: () => request<AiSettings>("GET", "/me/ai-settings"),
  updateAiSettings: (body: { baseUrl: string; model: string; apiKey?: string }) =>
    request<AiSettings>("PUT", "/me/ai-settings", body),
  clearAiSettings: () => request<void>("DELETE", "/me/ai-settings"),

  overview: () => request<Overview>("GET", "/stats/overview"),
  achievements: () => request<Achievement[]>("GET", "/stats/achievements"),
  topics: () => request<TopicProgress[]>("GET", "/stats/topics"),
  heatmap: (from: string, to: string) =>
    request<HeatmapEntry[]>("GET", `/stats/heatmap?from=${from}&to=${to}`),

  listGoals: (status?: string) =>
    request<Goal[]>("GET", `/goals${status ? `?status=${status}` : ""}`),
  getGoal: (id: string) => request<GoalTree>("GET", `/goals/${id}`),
  createGoal: (body: { title: string; description?: string; context?: string; target?: string }) =>
    request<Goal>("POST", "/goals", body),
  archiveGoal: (id: string) => request<Goal>("POST", `/goals/${id}/archive`),

  today: () => request<Task[]>("GET", "/tasks/today"),
  week: (start?: string) => request<Task[]>("GET", `/tasks/week${start ? `?start=${start}` : ""}`),
  planDay: () => request<Task[]>("POST", "/tasks/plan-day"),
  createTask: (body: { goalId: string; milestoneId?: string; title: string; estimateMinutes?: number; scheduledFor?: string }) =>
    request<Task>("POST", "/tasks", body),
  setTaskStatus: (id: string, status: string) =>
    request<Task>("PATCH", `/tasks/${id}/status`, { status }),
  scheduleTask: (id: string, scheduledFor: string | null) =>
    request<Task>("PATCH", `/tasks/${id}/schedule`, { scheduledFor }),

  startPlan: (body: { context: string; target: string; targetDate?: string; weeklyCapacityMinutes?: number }) =>
    request<PlanJob>("POST", "/ai/plans", body),
  parseRoadmap: (text: string) => request<PlanJob>("POST", "/ai/roadmaps/parse", { text }),
  decomposeTask: (taskId: string) => request<PlanJob>("POST", `/ai/tasks/${taskId}/decompose`),
  refinePlan: (goalId: string, message: string) =>
    request<PlanJob>("POST", `/ai/plans/${goalId}/refine`, { message }),
  getJob: (jobId: string) => request<AiJob>("GET", `/ai/jobs/${jobId}`),
  acceptPlan: (goalId: string) => request<GoalTree>("POST", `/ai/plans/${goalId}/accept`),
  replanGoal: (goalId: string) => request<PlanJob>("POST", `/ai/goals/${goalId}/replan`),
  acceptReplan: (goalId: string) => request<GoalTree>("POST", `/ai/goals/${goalId}/replan/accept`),
};
