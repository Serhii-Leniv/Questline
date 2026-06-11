-- Per-user AI provider settings (BYOK): a user can plug in their own OpenAI-compatible key
-- (OpenRouter, OpenAI, Groq, a local model, …). The API key is stored encrypted (AES-GCM).

alter table users add column ai_base_url     varchar(512);
alter table users add column ai_model        varchar(128);
alter table users add column ai_api_key_enc  text;
