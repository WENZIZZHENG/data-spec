-- ============================================================
-- DataSpec V9: 标准版本快照
-- ============================================================

CREATE TABLE IF NOT EXISTS ds_standard_snapshot (
    id              bigserial       PRIMARY KEY,
    project_id      bigint          NOT NULL,
    version         varchar(100)    NOT NULL,
    name            varchar(100),
    description     text,
    snapshot_hash   varchar(64)     NOT NULL,
    payload_json    text            NOT NULL,
    created_at      timestamp with time zone NOT NULL DEFAULT now(),
    updated_at      timestamp with time zone NOT NULL DEFAULT now(),
    is_deleted      boolean         NOT NULL DEFAULT false
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_standard_snapshot_project_version
    ON ds_standard_snapshot(project_id, version)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_standard_snapshot_project
    ON ds_standard_snapshot(project_id, created_at DESC)
    WHERE is_deleted = false;

ALTER TABLE ds_sql_check_record
    ADD COLUMN IF NOT EXISTS standard_snapshot_id bigint;

ALTER TABLE ds_sql_check_record
    ADD COLUMN IF NOT EXISTS standard_snapshot_version varchar(100);

ALTER TABLE ds_sql_check_record
    ADD COLUMN IF NOT EXISTS standard_snapshot_hash varchar(64);

COMMENT ON TABLE ds_standard_snapshot IS 'DataSpec 标准版本快照';
COMMENT ON COLUMN ds_standard_snapshot.version IS '用户定义的标准版本号';
COMMENT ON COLUMN ds_standard_snapshot.snapshot_hash IS '标准快照 payload 的 SHA-256 hash';
COMMENT ON COLUMN ds_standard_snapshot.payload_json IS '字段、枚举和规则的确定性 JSON 快照';
COMMENT ON COLUMN ds_sql_check_record.standard_snapshot_id IS '本次 SQL 检查引用的标准快照 ID';
COMMENT ON COLUMN ds_sql_check_record.standard_snapshot_version IS '本次 SQL 检查引用的标准版本';
COMMENT ON COLUMN ds_sql_check_record.standard_snapshot_hash IS '本次 SQL 检查引用的标准快照 hash';
