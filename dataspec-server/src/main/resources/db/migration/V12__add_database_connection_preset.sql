-- ============================================================
-- DataSpec V12: 数据库直连非敏感连接预设
-- ============================================================

CREATE TABLE IF NOT EXISTS ds_database_connection_preset (
    id                  bigserial       PRIMARY KEY,
    project_id          bigint          NOT NULL,
    name                varchar(100)    NOT NULL,
    database_type       varchar(20)     NOT NULL,
    host                varchar(200)    NOT NULL,
    port                integer         NOT NULL,
    database_name       varchar(200)    NOT NULL,
    schema_name         varchar(200),
    table_names_json    text,
    created_at          timestamp with time zone NOT NULL DEFAULT now(),
    updated_at          timestamp with time zone NOT NULL DEFAULT now(),
    is_deleted          boolean         NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_db_connection_preset_project_time
    ON ds_database_connection_preset(project_id, updated_at DESC)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_db_connection_preset_project_name
    ON ds_database_connection_preset(project_id, name)
    WHERE is_deleted = false;

COMMENT ON TABLE ds_database_connection_preset IS '数据库直连非敏感连接预设';
COMMENT ON COLUMN ds_database_connection_preset.name IS '预设别名';
COMMENT ON COLUMN ds_database_connection_preset.database_type IS '数据库类型,如 postgresql/mysql';
COMMENT ON COLUMN ds_database_connection_preset.table_names_json IS '默认表选择 JSON 数组,不包含凭据';
