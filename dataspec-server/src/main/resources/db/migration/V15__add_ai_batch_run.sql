-- ============================================================
-- DataSpec V15: AI 批量任务交付包
-- ============================================================

CREATE TABLE IF NOT EXISTS ds_ai_batch_run (
    id              bigserial       PRIMARY KEY,
    project_id      bigint          NOT NULL,
    batch_type      varchar(50)     NOT NULL,
    source          varchar(200),
    status          varchar(30)     NOT NULL DEFAULT 'SUCCESS',
    summary_json    text            NOT NULL,
    payload_json    text            NOT NULL,
    operator_name   varchar(100),
    created_at      timestamp without time zone NOT NULL DEFAULT localtimestamp,
    updated_at      timestamp without time zone NOT NULL DEFAULT localtimestamp,
    is_deleted      boolean         NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_ai_batch_run_project_time
    ON ds_ai_batch_run(project_id, created_at DESC)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_ai_batch_run_project_type
    ON ds_ai_batch_run(project_id, batch_type, created_at DESC)
    WHERE is_deleted = false;

COMMENT ON TABLE ds_ai_batch_run IS 'AI 批量任务交付包运行记录';
COMMENT ON COLUMN ds_ai_batch_run.batch_type IS '批量任务类型,第一版支持 SQL_LINT';
COMMENT ON COLUMN ds_ai_batch_run.source IS '任务来源,如 frontend/cli/local';
COMMENT ON COLUMN ds_ai_batch_run.summary_json IS '列表展示用摘要 JSON';
COMMENT ON COLUMN ds_ai_batch_run.payload_json IS '完整交付包 JSON,需经过敏感信息脱敏';
