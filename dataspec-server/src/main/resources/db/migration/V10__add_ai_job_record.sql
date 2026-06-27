-- ============================================================
-- DataSpec V10: AI 生成与修复决策回放记录
-- ============================================================

CREATE TABLE IF NOT EXISTS ds_ai_job_record (
    id                         bigserial       PRIMARY KEY,
    project_id                 bigint          NOT NULL,
    job_type                   varchar(50)     NOT NULL,
    title                      varchar(200),
    input_summary              varchar(500),
    prompt_version             varchar(100),
    status                     varchar(30)     NOT NULL DEFAULT 'SUCCESS',
    input_payload_json         text,
    output_payload_json        text,
    sql_check_record_id        bigint,
    standard_snapshot_id       bigint,
    standard_snapshot_version  varchar(100),
    standard_snapshot_hash     varchar(64),
    created_at                 timestamp with time zone NOT NULL DEFAULT now(),
    updated_at                 timestamp with time zone NOT NULL DEFAULT now(),
    is_deleted                 boolean         NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_ai_job_record_project_time
    ON ds_ai_job_record(project_id, created_at DESC)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_ai_job_record_project_type
    ON ds_ai_job_record(project_id, job_type, created_at DESC)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_ai_job_record_sql_check
    ON ds_ai_job_record(sql_check_record_id)
    WHERE is_deleted = false;

COMMENT ON TABLE ds_ai_job_record IS 'AI 生成与修复决策回放记录';
COMMENT ON COLUMN ds_ai_job_record.job_type IS 'AI 作业类型,如 CREATE_TABLE_PROMPT/FIX_SQL_PROMPT/SQL_LINT_FIX/DDL_PREVIEW';
COMMENT ON COLUMN ds_ai_job_record.input_payload_json IS '作业输入 JSON';
COMMENT ON COLUMN ds_ai_job_record.output_payload_json IS '作业输出 JSON';
COMMENT ON COLUMN ds_ai_job_record.sql_check_record_id IS '关联 SQL 检查记录 ID';
COMMENT ON COLUMN ds_ai_job_record.standard_snapshot_id IS '本次作业引用的标准快照 ID';
COMMENT ON COLUMN ds_ai_job_record.standard_snapshot_version IS '本次作业引用的标准版本';
COMMENT ON COLUMN ds_ai_job_record.standard_snapshot_hash IS '本次作业引用的标准快照 hash';
