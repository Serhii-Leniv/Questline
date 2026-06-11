// TypeScript mirrors of the backend DTOs (see com.questline.web.*).

export type GoalStatus = "ACTIVE" | "PAUSED" | "COMPLETED" | "ARCHIVED";
export type TaskStatus = "TODO" | "IN_PROGRESS" | "DONE" | "SKIPPED";
export type MilestoneStatus = "NOT_STARTED" | "IN_PROGRESS" | "DONE";
export type AiJobStatus = "PENDING" | "RUNNING" | "SUCCEEDED" | "FAILED";

export interface Me {
  id: string;
  email: string;
  name: string | null;
  image: string | null;
  timezone: string;
  dailyCapacityMinutes: number;
  dailyTaskGoal: number;
  xpTotal: number;
}

export interface Overview {
  xpTotal: number;
  level: number;
  currentStreak: number;
  longestStreak: number;
  freezesAvailable: number;
}

export interface Streak {
  current: number;
  longest: number;
  lastActiveDate: string | null;
  freezesAvailable: number;
}

export interface Achievement {
  code: string;
  title: string;
  description: string | null;
  icon: string | null;
  unlockedAt: string;
}

export interface TopicProgress {
  name: string;
  slug: string;
  total: number;
  done: number;
}

export interface HeatmapEntry {
  date: string;
  count: number;
}

export interface Goal {
  id: string;
  title: string;
  description: string | null;
  context: string | null;
  target: string | null;
  source: string;
  targetDate: string | null;
  status: GoalStatus;
  progress: number;
  createdAt: string;
  updatedAt: string;
}

export interface TaskNode {
  id: string;
  title: string;
  description: string | null;
  estimateMinutes: number | null;
  orderIndex: number;
  status: TaskStatus;
  scheduledFor: string | null;
  topics: string[];
  subtasks: TaskNode[];
}

export interface MilestoneNode {
  id: string;
  title: string;
  description: string | null;
  orderIndex: number;
  status: MilestoneStatus;
  progress: number;
  targetDate: string | null;
  tasks: TaskNode[];
}

export interface GoalTree {
  id: string;
  title: string;
  description: string | null;
  context: string | null;
  target: string | null;
  status: GoalStatus;
  progress: number;
  milestones: MilestoneNode[];
}

export interface Task {
  id: string;
  goalId: string;
  milestoneId: string | null;
  title: string;
  description: string | null;
  estimateMinutes: number | null;
  orderIndex: number;
  status: TaskStatus;
  scheduledFor: string | null;
  completedAt: string | null;
  topics: string[];
}

export interface PlanJob {
  jobId: string;
  goalId: string;
}

// The generated plan as stored on the job (raw shape from the ai/ records).
export interface GeneratedPlan {
  summary: string;
  milestones: { title: string; description: string | null; tasks: { title: string; description: string | null; estimateMinutes: number | null }[] }[];
}

export interface AiJob {
  id: string;
  type: string;
  status: AiJobStatus;
  error: string | null;
  plan: GeneratedPlan | null;
}
