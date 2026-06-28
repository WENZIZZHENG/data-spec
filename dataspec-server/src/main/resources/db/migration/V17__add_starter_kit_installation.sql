-- ============================================================
-- DataSpec V17: 领域 Starter Kit 安装记录
-- ============================================================

CREATE TABLE IF NOT EXISTS ds_starter_kit_installation (
    id                bigserial       PRIMARY KEY,
    project_id        bigint          NOT NULL,
    kit_key           varchar(100)    NOT NULL,
    kit_version       varchar(50)     NOT NULL,
    kit_name          varchar(100)    NOT NULL,
    created_counts_json text,
    skipped_counts_json text,
    warnings_json     text,
    operator_name     varchar(100),
    applied_at        timestamp without time zone NOT NULL DEFAULT localtimestamp,
    created_at        timestamp without time zone NOT NULL DEFAULT localtimestamp,
    updated_at        timestamp without time zone NOT NULL DEFAULT localtimestamp,
    is_deleted        boolean         NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_starter_kit_installation_project
    ON ds_starter_kit_installation(project_id, applied_at DESC)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_starter_kit_installation_project_kit
    ON ds_starter_kit_installation(project_id, kit_key, kit_version)
    WHERE is_deleted = false;

COMMENT ON TABLE ds_starter_kit_installation IS '领域 Starter Kit 安装摘要记录';
COMMENT ON COLUMN ds_starter_kit_installation.kit_key IS 'Starter Kit 编码,如 user_account/order_trade';
COMMENT ON COLUMN ds_starter_kit_installation.kit_version IS 'Starter Kit 版本';
COMMENT ON COLUMN ds_starter_kit_installation.created_counts_json IS '本次应用创建的字段/枚举/模板数量 JSON';
COMMENT ON COLUMN ds_starter_kit_installation.skipped_counts_json IS '本次应用跳过的字段/枚举/模板数量 JSON';
COMMENT ON COLUMN ds_starter_kit_installation.warnings_json IS '本次应用的非阻断警告 JSON';
