-- ============================================================
-- 个人/小团队安全基线
-- ============================================================

CREATE TABLE IF NOT EXISTS ds_api_token (
    id              bigserial       PRIMARY KEY,
    name            varchar(100)    NOT NULL,
    token_hash      varchar(64)     NOT NULL,
    operator_name   varchar(100)    NOT NULL,
    project_ids     text            NOT NULL DEFAULT '*',
    enabled         boolean         NOT NULL DEFAULT true,
    created_at      timestamp with time zone NOT NULL DEFAULT now(),
    updated_at      timestamp with time zone NOT NULL DEFAULT now(),
    is_deleted      boolean         NOT NULL DEFAULT false
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_api_token_hash
    ON ds_api_token(token_hash)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_api_token_enabled
    ON ds_api_token(enabled)
    WHERE is_deleted = false;

ALTER TABLE ds_standard_change_log
    ADD COLUMN IF NOT EXISTS operator_name varchar(100) NOT NULL DEFAULT 'local';

COMMENT ON TABLE ds_api_token IS 'DataSpec API Token';
COMMENT ON COLUMN ds_api_token.token_hash IS 'API token 的 SHA-256 hash,不保存明文 token';
COMMENT ON COLUMN ds_api_token.operator_name IS '该 token 对应的操作者名称';
COMMENT ON COLUMN ds_api_token.project_ids IS '授权项目 ID 列表,逗号分隔; * 表示全部项目';
COMMENT ON COLUMN ds_standard_change_log.operator_name IS '执行本次标准变更的操作者';
