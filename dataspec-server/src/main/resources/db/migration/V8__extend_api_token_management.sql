-- ============================================================
-- DataSpec V8: API Token 管理字段
-- ============================================================

ALTER TABLE ds_api_token
    ADD COLUMN IF NOT EXISTS last_used_at timestamp with time zone;

ALTER TABLE ds_api_token
    ADD COLUMN IF NOT EXISTS disabled_at timestamp with time zone;

COMMENT ON COLUMN ds_api_token.last_used_at IS 'API token 最近一次认证成功时间';
COMMENT ON COLUMN ds_api_token.disabled_at IS 'API token 停用时间';
