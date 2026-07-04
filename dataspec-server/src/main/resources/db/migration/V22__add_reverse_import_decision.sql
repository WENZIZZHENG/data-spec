-- ============================================================
-- 反向导入字段映射决策
-- ============================================================

CREATE TABLE IF NOT EXISTS ds_reverse_import_decision (
    id                  bigserial       PRIMARY KEY,
    project_id          bigint          NOT NULL,
    batch_id            bigint          NOT NULL,
    source_type         varchar(32)     NOT NULL,
    schema_name         varchar(128),
    table_name          varchar(128),
    column_name         varchar(128)    NOT NULL,
    data_type           varchar(200),
    decision_type       varchar(32)     NOT NULL,
    matched_field_id    bigint,
    matched_field_name  varchar(128),
    match_reason        text,
    confidence          numeric(5,4),
    ignore_reason       text,
    confirm_reason      text,
    metadata_json       text,
    created_at          timestamp without time zone NOT NULL DEFAULT localtimestamp
);

CREATE INDEX IF NOT EXISTS idx_reverse_import_decision_project
    ON ds_reverse_import_decision(project_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_reverse_import_decision_batch
    ON ds_reverse_import_decision(batch_id);

CREATE INDEX IF NOT EXISTS idx_reverse_import_decision_field
    ON ds_reverse_import_decision(matched_field_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_reverse_import_decision_column
    ON ds_reverse_import_decision(project_id, table_name, column_name);

COMMENT ON TABLE ds_reverse_import_decision IS '数据库反向导入字段映射决策';
COMMENT ON COLUMN ds_reverse_import_decision.decision_type IS '字段映射决策类型: EXISTING_MATCH/NEW_CANDIDATE/IMPORTED/SKIPPED_EXISTING/IGNORED';
COMMENT ON COLUMN ds_reverse_import_decision.match_reason IS '字段命中、候选生成或确认导入的结构化理由';
COMMENT ON COLUMN ds_reverse_import_decision.confirm_reason IS '用户确认导入时填写的理由';
COMMENT ON COLUMN ds_reverse_import_decision.ignore_reason IS '用户忽略候选或系统跳过候选的理由';
COMMENT ON COLUMN ds_reverse_import_decision.metadata_json IS '候选字段 metadata JSON 快照,不得包含数据库密码、token、JDBC URL 或业务数据行';
