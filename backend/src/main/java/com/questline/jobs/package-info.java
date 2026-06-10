/**
 * JobRunr background-job services and recurring tasks.
 *
 * <p>Phase 0 only configures JobRunr (Postgres storage provider, auto-selected from the Spring
 * {@code DataSource}; see {@code application.yml} under {@code org.jobrunr.*}). No jobs are
 * defined yet — durable LLM jobs and recurring streak/notification tasks arrive in Phase 1+.
 */
package com.questline.jobs;
