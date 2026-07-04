-- ============================================================
-- AI 任务运行状态与断点恢复诊断
-- ============================================================

CREATE TABLE IF NOT EXISTS ds_ai_task_run (
    id                     bigserial       PRIMARY KEY,
    project_id             bigint          NOT NULL,
    task_type              varchar(64)     NOT NULL,
    source_type            varchar(64),
    source_id              bigint,
    status                 varchar(32)     NOT NULL,
    input_hash             varchar(128),
    idempotency_key        varchar(200),
    step_status_json       text,
    retryable              boolean         NOT NULL DEFAULT false,
    failed_step            varchar(128),
    resume_command         text,
    next_action            text,
    partial_artifacts_json text,
    metadata_json          text,
    operator_name          varchar(128),
    started_at             timestamp without time zone NOT NULL DEFAULT localtimestamp,
    finished_at            timestamp without time zone,
    expires_at             timestamp without time zone,
    created_at             timestamp without time zone NOT NULL DEFAULT localtimestamp,
    updated_at             timestamp without time zone NOT NULL DEFAULT localtimestamp,
    is_deleted             boolean         NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_ai_task_run_project_status
    ON ds_ai_task_run(project_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_ai_task_run_project_type
    ON ds_ai_task_run(project_id, task_type, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_ai_task_run_source
    ON ds_ai_task_run(source_type, source_id);

CREATE INDEX IF NOT EXISTS idx_ai_task_run_input_hash
    ON ds_ai_task_run(project_id, task_type, input_hash);

COMMENT ON TABLE ds_ai_task_run IS 'AI 任务运行状态与断点恢复诊断';
COMMENT ON COLUMN ds_ai_task_run.status IS '任务状态: RUNNING/SUCCEEDED/PARTIAL_FAILED/FAILED/CANCELLED/EXPIRED';
COMMENT ON COLUMN ds_ai_task_run.input_hash IS '任务输入摘要 hash,用于重复重试判断';
COMMENT ON COLUMN ds_ai_task_run.idempotency_key IS '调用方传入的幂等 key 摘要或脱敏值';
COMMENT ON COLUMN ds_ai_task_run.step_status_json IS '步骤状态 JSON,不得包含 token、密码、完整 JDBC URL 或业务数据行';
COMMENT ON COLUMN ds_ai_task_run.partial_artifacts_json IS '已完成 artifact 摘要 JSON,不得包含敏感凭据或业务数据行';
COMMENT ON COLUMN ds_ai_task_run.metadata_json IS '任务恢复 metadata JSON,不得包含敏感凭据或业务数据行';
