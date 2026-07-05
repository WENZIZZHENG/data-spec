-- ============================================================
-- DataSpec V26: 标准复用包与应用记录
-- ============================================================

CREATE TABLE IF NOT EXISTS ds_standard_reuse_pack (
    id                    bigserial       PRIMARY KEY,
    project_id             bigint          NOT NULL,
    source_project_name    varchar(200),
    pack_key               varchar(100)    NOT NULL,
    pack_name              varchar(200)    NOT NULL,
    base_pack_version      varchar(100)    NOT NULL,
    description            text,
    package_hash           varchar(64)     NOT NULL,
    payload_json           text            NOT NULL,
    asset_counts_json      text            NOT NULL,
    created_at             timestamp without time zone NOT NULL DEFAULT localtimestamp,
    updated_at             timestamp without time zone NOT NULL DEFAULT localtimestamp,
    is_deleted             boolean         NOT NULL DEFAULT false
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_standard_reuse_pack_project_key_version
    ON ds_standard_reuse_pack(project_id, pack_key, base_pack_version)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_standard_reuse_pack_project
    ON ds_standard_reuse_pack(project_id, created_at DESC)
    WHERE is_deleted = false;

CREATE TABLE IF NOT EXISTS ds_standard_reuse_pack_application (
    id                    bigserial       PRIMARY KEY,
    project_id             bigint          NOT NULL,
    pack_id                bigint,
    pack_key               varchar(100)    NOT NULL,
    pack_name              varchar(200)    NOT NULL,
    base_pack_version      varchar(100)    NOT NULL,
    package_hash           varchar(64)     NOT NULL,
    source_project_id      bigint,
    source_project_name    varchar(200),
    created_counts_json    text,
    skipped_counts_json    text,
    drift_counts_json      text,
    drift_report_json      text,
    operator_name          varchar(100),
    applied_at             timestamp without time zone NOT NULL DEFAULT localtimestamp,
    created_at             timestamp without time zone NOT NULL DEFAULT localtimestamp,
    updated_at             timestamp without time zone NOT NULL DEFAULT localtimestamp,
    is_deleted             boolean         NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_standard_reuse_pack_application_project
    ON ds_standard_reuse_pack_application(project_id, applied_at DESC)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_standard_reuse_pack_application_pack
    ON ds_standard_reuse_pack_application(pack_id)
    WHERE is_deleted = false;

COMMENT ON TABLE ds_standard_reuse_pack IS '项目标准复用包';
COMMENT ON COLUMN ds_standard_reuse_pack.project_id IS '源项目 ID';
COMMENT ON COLUMN ds_standard_reuse_pack.pack_key IS '项目内稳定包 key';
COMMENT ON COLUMN ds_standard_reuse_pack.base_pack_version IS '用户定义共享包版本';
COMMENT ON COLUMN ds_standard_reuse_pack.package_hash IS '复用包 payload 的 SHA-256 hash';
COMMENT ON COLUMN ds_standard_reuse_pack.payload_json IS '字段、枚举、规则和模板的确定性 JSON,不包含数据库 ID 或源库行值';
COMMENT ON COLUMN ds_standard_reuse_pack.asset_counts_json IS '包内资产数量摘要 JSON';

COMMENT ON TABLE ds_standard_reuse_pack_application IS '标准复用包应用摘要';
COMMENT ON COLUMN ds_standard_reuse_pack_application.project_id IS '目标项目 ID';
COMMENT ON COLUMN ds_standard_reuse_pack_application.package_hash IS '应用时的复用包内容 hash';
COMMENT ON COLUMN ds_standard_reuse_pack_application.created_counts_json IS '本次应用创建的资产数量 JSON';
COMMENT ON COLUMN ds_standard_reuse_pack_application.skipped_counts_json IS '本次应用跳过的资产数量 JSON';
COMMENT ON COLUMN ds_standard_reuse_pack_application.drift_counts_json IS '本次应用后的漂移计数 JSON';
COMMENT ON COLUMN ds_standard_reuse_pack_application.drift_report_json IS '本次应用后的漂移报告 JSON,不保存 raw secret 或源库行值';
