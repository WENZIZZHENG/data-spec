-- ============================================================
-- DataSpec V4: SQL 检查记录表
-- 用于保存每次 SQL 校验的原 SQL、修正 SQL、问题统计与结构化结果,
-- 供个人复盘和后续命中率报告(P2-7)使用。
-- ============================================================

CREATE TABLE IF NOT EXISTS ds_sql_check_record (
    id               bigserial       PRIMARY KEY,
    project_id       bigint,
    original_sql     text            NOT NULL,
    fixed_sql        text,
    error_count      integer         NOT NULL DEFAULT 0,
    warning_count    integer         NOT NULL DEFAULT 0,
    suggestion_count integer         NOT NULL DEFAULT 0,
    issues_json      text,
    created_at       timestamp with time zone NOT NULL DEFAULT now(),
    updated_at       timestamp with time zone NOT NULL DEFAULT now(),
    is_deleted       boolean         NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_sql_check_record_project
    ON ds_sql_check_record(project_id) WHERE is_deleted = false;

COMMENT ON TABLE ds_sql_check_record IS 'SQL 检查记录';
COMMENT ON COLUMN ds_sql_check_record.original_sql IS '原始 SQL 文本';
COMMENT ON COLUMN ds_sql_check_record.fixed_sql IS '基于确定性修复建议重建的修正 SQL';
COMMENT ON COLUMN ds_sql_check_record.issues_json IS '结构化校验问题 JSON';
