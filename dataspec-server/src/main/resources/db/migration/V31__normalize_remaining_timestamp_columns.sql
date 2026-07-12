-- ============================================================
-- DataSpec 后续时间列类型修正
--
-- V2 已确立 Java LocalDateTime 对应 PostgreSQL timestamp without time zone。
-- V4 之后的部分迁移重新使用了 timestamptz，PostgreSQL JDBC 不允许将其
-- 直接读取为 LocalDateTime。本迁移沿用 V2 的墙钟时间语义，仅处理明确
-- 由 DataSpec LocalDateTime 字段承载的列，不扫描或修改其他 schema。
-- ============================================================

ALTER TABLE ds_sql_check_record
    ALTER COLUMN created_at TYPE timestamp without time zone USING created_at AT TIME ZONE 'Asia/Shanghai',
    ALTER COLUMN updated_at TYPE timestamp without time zone USING updated_at AT TIME ZONE 'Asia/Shanghai',
    ALTER COLUMN created_at SET DEFAULT localtimestamp,
    ALTER COLUMN updated_at SET DEFAULT localtimestamp;

ALTER TABLE ds_standard_change_log
    ALTER COLUMN changed_at TYPE timestamp without time zone USING changed_at AT TIME ZONE 'Asia/Shanghai',
    ALTER COLUMN changed_at SET DEFAULT localtimestamp;

ALTER TABLE ds_api_token
    ALTER COLUMN created_at TYPE timestamp without time zone USING created_at AT TIME ZONE 'Asia/Shanghai',
    ALTER COLUMN updated_at TYPE timestamp without time zone USING updated_at AT TIME ZONE 'Asia/Shanghai',
    ALTER COLUMN last_used_at TYPE timestamp without time zone USING last_used_at AT TIME ZONE 'Asia/Shanghai',
    ALTER COLUMN disabled_at TYPE timestamp without time zone USING disabled_at AT TIME ZONE 'Asia/Shanghai',
    ALTER COLUMN created_at SET DEFAULT localtimestamp,
    ALTER COLUMN updated_at SET DEFAULT localtimestamp;

ALTER TABLE ds_field_source
    ALTER COLUMN created_at TYPE timestamp without time zone USING created_at AT TIME ZONE 'Asia/Shanghai',
    ALTER COLUMN created_at SET DEFAULT localtimestamp;

ALTER TABLE ds_reverse_import_batch
    ALTER COLUMN created_at TYPE timestamp without time zone USING created_at AT TIME ZONE 'Asia/Shanghai',
    ALTER COLUMN created_at SET DEFAULT localtimestamp;

ALTER TABLE ds_standard_snapshot
    ALTER COLUMN created_at TYPE timestamp without time zone USING created_at AT TIME ZONE 'Asia/Shanghai',
    ALTER COLUMN updated_at TYPE timestamp without time zone USING updated_at AT TIME ZONE 'Asia/Shanghai',
    ALTER COLUMN created_at SET DEFAULT localtimestamp,
    ALTER COLUMN updated_at SET DEFAULT localtimestamp;

ALTER TABLE ds_ai_job_record
    ALTER COLUMN created_at TYPE timestamp without time zone USING created_at AT TIME ZONE 'Asia/Shanghai',
    ALTER COLUMN updated_at TYPE timestamp without time zone USING updated_at AT TIME ZONE 'Asia/Shanghai',
    ALTER COLUMN created_at SET DEFAULT localtimestamp,
    ALTER COLUMN updated_at SET DEFAULT localtimestamp;

ALTER TABLE ds_rule_exemption
    ALTER COLUMN expires_at TYPE timestamp without time zone USING expires_at AT TIME ZONE 'Asia/Shanghai',
    ALTER COLUMN created_at TYPE timestamp without time zone USING created_at AT TIME ZONE 'Asia/Shanghai',
    ALTER COLUMN updated_at TYPE timestamp without time zone USING updated_at AT TIME ZONE 'Asia/Shanghai',
    ALTER COLUMN created_at SET DEFAULT localtimestamp,
    ALTER COLUMN updated_at SET DEFAULT localtimestamp;

ALTER TABLE ds_database_connection_preset
    ALTER COLUMN created_at TYPE timestamp without time zone USING created_at AT TIME ZONE 'Asia/Shanghai',
    ALTER COLUMN updated_at TYPE timestamp without time zone USING updated_at AT TIME ZONE 'Asia/Shanghai',
    ALTER COLUMN created_at SET DEFAULT localtimestamp,
    ALTER COLUMN updated_at SET DEFAULT localtimestamp;

ALTER TABLE ds_rule_baseline
    ALTER COLUMN applied_at TYPE timestamp without time zone USING applied_at AT TIME ZONE 'Asia/Shanghai',
    ALTER COLUMN created_at TYPE timestamp without time zone USING created_at AT TIME ZONE 'Asia/Shanghai',
    ALTER COLUMN updated_at TYPE timestamp without time zone USING updated_at AT TIME ZONE 'Asia/Shanghai',
    ALTER COLUMN applied_at SET DEFAULT localtimestamp,
    ALTER COLUMN created_at SET DEFAULT localtimestamp,
    ALTER COLUMN updated_at SET DEFAULT localtimestamp;

ALTER TABLE ds_project_restore_record
    ALTER COLUMN created_at TYPE timestamp without time zone USING created_at AT TIME ZONE 'Asia/Shanghai',
    ALTER COLUMN created_at SET DEFAULT localtimestamp;

ALTER TABLE ds_business_object_standard
    ALTER COLUMN created_at TYPE timestamp without time zone USING created_at AT TIME ZONE 'Asia/Shanghai',
    ALTER COLUMN updated_at TYPE timestamp without time zone USING updated_at AT TIME ZONE 'Asia/Shanghai',
    ALTER COLUMN created_at SET DEFAULT localtimestamp,
    ALTER COLUMN updated_at SET DEFAULT localtimestamp;

ALTER TABLE ds_field_semantic_rule
    ALTER COLUMN created_at TYPE timestamp without time zone USING created_at AT TIME ZONE 'Asia/Shanghai',
    ALTER COLUMN updated_at TYPE timestamp without time zone USING updated_at AT TIME ZONE 'Asia/Shanghai',
    ALTER COLUMN created_at SET DEFAULT localtimestamp,
    ALTER COLUMN updated_at SET DEFAULT localtimestamp;

ALTER TABLE ds_metric_definition
    ALTER COLUMN created_at TYPE timestamp without time zone USING created_at AT TIME ZONE 'Asia/Shanghai',
    ALTER COLUMN updated_at TYPE timestamp without time zone USING updated_at AT TIME ZONE 'Asia/Shanghai',
    ALTER COLUMN created_at SET DEFAULT localtimestamp,
    ALTER COLUMN updated_at SET DEFAULT localtimestamp;

COMMENT ON COLUMN ds_sql_check_record.created_at IS 'SQL 检查记录创建时间，无时区本地时间，与 Java LocalDateTime 对应';
COMMENT ON COLUMN ds_sql_check_record.updated_at IS 'SQL 检查记录更新时间，无时区本地时间，与 Java LocalDateTime 对应';
COMMENT ON COLUMN ds_standard_change_log.changed_at IS '标准变更发生时间，无时区本地时间，与 Java LocalDateTime 对应';
COMMENT ON COLUMN ds_api_token.created_at IS 'Token 创建时间，无时区本地时间，与 Java LocalDateTime 对应';
COMMENT ON COLUMN ds_api_token.updated_at IS 'Token 更新时间，无时区本地时间，与 Java LocalDateTime 对应';
COMMENT ON COLUMN ds_api_token.last_used_at IS 'Token 最近使用时间，可空的无时区本地时间';
COMMENT ON COLUMN ds_api_token.disabled_at IS 'Token 禁用时间，可空的无时区本地时间';
COMMENT ON COLUMN ds_field_source.created_at IS '字段来源记录创建时间，无时区本地时间';
COMMENT ON COLUMN ds_reverse_import_batch.created_at IS '反向导入批次创建时间，无时区本地时间';
COMMENT ON COLUMN ds_standard_snapshot.created_at IS '标准快照创建时间，无时区本地时间';
COMMENT ON COLUMN ds_standard_snapshot.updated_at IS '标准快照更新时间，无时区本地时间';
COMMENT ON COLUMN ds_ai_job_record.created_at IS 'AI 任务记录创建时间，无时区本地时间';
COMMENT ON COLUMN ds_ai_job_record.updated_at IS 'AI 任务记录更新时间，无时区本地时间';
COMMENT ON COLUMN ds_rule_exemption.expires_at IS '规则豁免过期时间，可空的无时区本地时间';
COMMENT ON COLUMN ds_rule_exemption.created_at IS '规则豁免创建时间，无时区本地时间';
COMMENT ON COLUMN ds_rule_exemption.updated_at IS '规则豁免更新时间，无时区本地时间';
COMMENT ON COLUMN ds_database_connection_preset.created_at IS '数据库连接预设创建时间，无时区本地时间';
COMMENT ON COLUMN ds_database_connection_preset.updated_at IS '数据库连接预设更新时间，无时区本地时间';
COMMENT ON COLUMN ds_rule_baseline.applied_at IS '规则基线应用时间，无时区本地时间';
COMMENT ON COLUMN ds_rule_baseline.created_at IS '规则基线创建时间，无时区本地时间';
COMMENT ON COLUMN ds_rule_baseline.updated_at IS '规则基线更新时间，无时区本地时间';
COMMENT ON COLUMN ds_project_restore_record.created_at IS '项目恢复记录创建时间，无时区本地时间';
COMMENT ON COLUMN ds_business_object_standard.created_at IS '业务对象标准创建时间，无时区本地时间';
COMMENT ON COLUMN ds_business_object_standard.updated_at IS '业务对象标准更新时间，无时区本地时间';
COMMENT ON COLUMN ds_field_semantic_rule.created_at IS '字段语义规则创建时间，无时区本地时间';
COMMENT ON COLUMN ds_field_semantic_rule.updated_at IS '字段语义规则更新时间，无时区本地时间';
COMMENT ON COLUMN ds_metric_definition.created_at IS '指标定义创建时间，无时区本地时间';
COMMENT ON COLUMN ds_metric_definition.updated_at IS '指标定义更新时间，无时区本地时间';
