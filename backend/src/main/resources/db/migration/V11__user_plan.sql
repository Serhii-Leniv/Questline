-- Subscription plan per user (Phase 3 billing). Drives AI usage limits.
alter table users add column plan varchar(16) not null default 'FREE';
