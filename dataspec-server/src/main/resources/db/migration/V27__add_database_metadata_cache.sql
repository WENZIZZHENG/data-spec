-- ============================================================
-- DataSpec V27: 数据库 metadata 增量缓存
-- ============================================================

CREATE TABLE IF NOT EXISTS ds_database_metadata_cache (
    id                      bigserial       PRIMARY KEY,
    project_id              bigint          NOT NULL,
    preset_id               bigint,
    source_scope_hash       char(64)        NOT NULL,
    database_type           varchar(32)     NOT NULL,
    database_name           varchar(200),
    schema_name             varchar(200),
    table_name              varchar(200)    NOT NULL,
    table_fingerprint       char(64)        NOT NULL,
    metadata_json           text            NOT NULL,
    source_product_name     varchar(200),
    source_product_version  varchar(500),
    first_seen_at           timestamp without time zone NOT NULL DEFAULT localtimestamp,
    last_seen_at            timestamp without time zone NOT NULL DEFAULT localtimestamp,
    expires_at              timestamp without time zone NOT NULL,
    refresh_mode            varchar(20)     NOT NULL DEFAULT 'AUTO',
    change_summary_json     text,
    created_at              timestamp without time zone NOT NULL DEFAULT localtimestamp,
    updated_at              timestamp without time zone NOT NULL DEFAULT localtimestamp,
    is_deleted              boolean         NOT NULL DEFAULT false
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_database_metadata_cache_scope_table
    ON ds_database_metadata_cache(project_id, source_scope_hash, schema_name, table_name)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_database_metadata_cache_project_seen
    ON ds_database_metadata_cache(project_id, last_seen_at DESC)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_database_metadata_cache_expires
    ON ds_database_metadata_cache(expires_at)
    WHERE is_deleted = false;

COMMENT ON TABLE ds_database_metadata_cache IS '数据库 schema-only metadata 增量缓存,不得保存密码、token、JDBC URL 或业务数据行';
COMMENT ON COLUMN ds_database_metadata_cache.project_id IS '所属 DataSpec 项目 ID,用于隔离不同项目的结构缓存';
COMMENT ON COLUMN ds_database_metadata_cache.preset_id IS '可选数据库连接预设 ID;为空时使用 source_scope_hash 隔离来源';
COMMENT ON COLUMN ds_database_metadata_cache.source_scope_hash IS '由非密码连接字段规范化后计算的 SHA-256 hash,不可反推出凭据';
COMMENT ON COLUMN ds_database_metadata_cache.database_type IS '数据库类型,如 POSTGRESQL/MYSQL';
COMMENT ON COLUMN ds_database_metadata_cache.database_name IS '数据库名脱敏摘要,不得保存完整连接串';
COMMENT ON COLUMN ds_database_metadata_cache.schema_name IS 'schema 名;MySQL 场景可能为空';
COMMENT ON COLUMN ds_database_metadata_cache.table_name IS '表名,与项目、来源 hash 和 schema 共同组成缓存唯一边界';
COMMENT ON COLUMN ds_database_metadata_cache.table_fingerprint IS '当前表结构规范化后的 SHA-256 fingerprint';
COMMENT ON COLUMN ds_database_metadata_cache.metadata_json IS '表结构 metadata JSON,仅包含表、字段、索引、注释等 schema 信息';
COMMENT ON COLUMN ds_database_metadata_cache.source_product_name IS '源数据库产品名脱敏摘要';
COMMENT ON COLUMN ds_database_metadata_cache.source_product_version IS '源数据库版本脱敏摘要';
COMMENT ON COLUMN ds_database_metadata_cache.first_seen_at IS '首次看见该表结构来源的时间';
COMMENT ON COLUMN ds_database_metadata_cache.last_seen_at IS '最近一次从源库刷新或确认该表结构的时间';
COMMENT ON COLUMN ds_database_metadata_cache.expires_at IS '缓存过期时间';
COMMENT ON COLUMN ds_database_metadata_cache.refresh_mode IS '最近一次写入缓存使用的刷新策略:AUTO/REFRESH/BYPASS';
COMMENT ON COLUMN ds_database_metadata_cache.change_summary_json IS '最近一次刷新生成的结构变化摘要 JSON,不包含业务数据行';
COMMENT ON COLUMN ds_database_metadata_cache.created_at IS '缓存记录创建时间,使用 timestamp without time zone 兼容 Java LocalDateTime';
COMMENT ON COLUMN ds_database_metadata_cache.updated_at IS '缓存记录更新时间,使用 timestamp without time zone 兼容 Java LocalDateTime';
COMMENT ON COLUMN ds_database_metadata_cache.is_deleted IS '逻辑删除标记;删除表的缓存优先置为过期而不是立即删除';
