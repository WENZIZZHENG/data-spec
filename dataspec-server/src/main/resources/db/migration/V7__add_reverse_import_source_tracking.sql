-- ============================================================
-- 反向导入来源与批次追踪
-- ============================================================

CREATE TABLE IF NOT EXISTS ds_reverse_import_batch (
    id                bigserial       PRIMARY KEY,
    project_id        bigint          NOT NULL,
    source_type       varchar(32)     NOT NULL,
    database_type     varchar(32),
    database_name     varchar(128),
    schema_name       varchar(128),
    table_names_json  text,
    imported_count    integer         NOT NULL DEFAULT 0,
    skipped_count     integer         NOT NULL DEFAULT 0,
    operator_name     varchar(100)    NOT NULL DEFAULT 'local',
    created_at        timestamp with time zone NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS ds_field_source (
    id                bigserial       PRIMARY KEY,
    project_id        bigint          NOT NULL,
    field_id          bigint          NOT NULL,
    batch_id          bigint          NOT NULL,
    source_type       varchar(32)     NOT NULL,
    schema_name       varchar(128),
    table_name        varchar(128),
    column_name       varchar(128)    NOT NULL,
    data_type         varchar(200),
    nullable          boolean,
    default_value     text,
    comment           text,
    metadata_json     text,
    created_at        timestamp with time zone NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_reverse_import_batch_project
    ON ds_reverse_import_batch(project_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_field_source_field
    ON ds_field_source(field_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_field_source_batch
    ON ds_field_source(batch_id);

CREATE INDEX IF NOT EXISTS idx_field_source_project
    ON ds_field_source(project_id, created_at DESC);

COMMENT ON TABLE ds_reverse_import_batch IS '数据库反向导入批次';
COMMENT ON COLUMN ds_reverse_import_batch.source_type IS '来源类型,如 database';
COMMENT ON COLUMN ds_reverse_import_batch.table_names_json IS '本次选择表名 JSON,不包含密码或完整连接串';
COMMENT ON TABLE ds_field_source IS '标准字段来源追踪';
COMMENT ON COLUMN ds_field_source.metadata_json IS '字段导入候选原始 metadata JSON 快照';
