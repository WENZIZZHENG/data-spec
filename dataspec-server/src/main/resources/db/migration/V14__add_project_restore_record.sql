-- ============================================================
-- DataSpec V14: 项目备份恢复摘要记录
-- ============================================================

CREATE TABLE IF NOT EXISTS ds_project_restore_record (
    id                    bigserial       PRIMARY KEY,
    project_id             bigint          NOT NULL,
    package_hash           varchar(128)    NOT NULL,
    source_project_name    varchar(200),
    source_project_id      bigint,
    schema_version         integer         NOT NULL,
    dry_run                boolean         NOT NULL DEFAULT false,
    overwrite              boolean         NOT NULL DEFAULT false,
    created_count          integer         NOT NULL DEFAULT 0,
    updated_count          integer         NOT NULL DEFAULT 0,
    skipped_count          integer         NOT NULL DEFAULT 0,
    conflict_count         integer         NOT NULL DEFAULT 0,
    blocked_count          integer         NOT NULL DEFAULT 0,
    warning_count          integer         NOT NULL DEFAULT 0,
    summary_json           text            NOT NULL,
    operator_name          varchar(100),
    created_at             timestamp with time zone NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_project_restore_record_project
    ON ds_project_restore_record(project_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_project_restore_record_hash
    ON ds_project_restore_record(package_hash);

COMMENT ON TABLE ds_project_restore_record IS '项目备份恢复摘要记录';
COMMENT ON COLUMN ds_project_restore_record.package_hash IS '备份包内容 hash';
COMMENT ON COLUMN ds_project_restore_record.summary_json IS '恢复计划和执行摘要,不保存完整备份包';
