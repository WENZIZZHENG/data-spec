-- ============================================================
-- DataSpec 时间列类型修正
-- Java 实体当前统一使用 LocalDateTime, PostgreSQL 侧使用 timestamp without time zone
-- 避免 MyBatis 从 timestamptz 读取为 LocalDateTime 时抛类型转换异常
-- ============================================================

ALTER TABLE ds_project
    ALTER COLUMN created_at TYPE timestamp without time zone USING created_at::timestamp,
    ALTER COLUMN updated_at TYPE timestamp without time zone USING updated_at::timestamp,
    ALTER COLUMN created_at SET DEFAULT localtimestamp,
    ALTER COLUMN updated_at SET DEFAULT localtimestamp;

ALTER TABLE ds_field
    ALTER COLUMN created_at TYPE timestamp without time zone USING created_at::timestamp,
    ALTER COLUMN updated_at TYPE timestamp without time zone USING updated_at::timestamp,
    ALTER COLUMN created_at SET DEFAULT localtimestamp,
    ALTER COLUMN updated_at SET DEFAULT localtimestamp;

ALTER TABLE ds_domain
    ALTER COLUMN created_at TYPE timestamp without time zone USING created_at::timestamp,
    ALTER COLUMN updated_at TYPE timestamp without time zone USING updated_at::timestamp,
    ALTER COLUMN created_at SET DEFAULT localtimestamp,
    ALTER COLUMN updated_at SET DEFAULT localtimestamp;

ALTER TABLE ds_enum_dict
    ALTER COLUMN created_at TYPE timestamp without time zone USING created_at::timestamp,
    ALTER COLUMN updated_at TYPE timestamp without time zone USING updated_at::timestamp,
    ALTER COLUMN created_at SET DEFAULT localtimestamp,
    ALTER COLUMN updated_at SET DEFAULT localtimestamp;

ALTER TABLE ds_enum_value
    ALTER COLUMN created_at TYPE timestamp without time zone USING created_at::timestamp,
    ALTER COLUMN updated_at TYPE timestamp without time zone USING updated_at::timestamp,
    ALTER COLUMN created_at SET DEFAULT localtimestamp,
    ALTER COLUMN updated_at SET DEFAULT localtimestamp;

ALTER TABLE ds_template
    ALTER COLUMN created_at TYPE timestamp without time zone USING created_at::timestamp,
    ALTER COLUMN updated_at TYPE timestamp without time zone USING updated_at::timestamp,
    ALTER COLUMN created_at SET DEFAULT localtimestamp,
    ALTER COLUMN updated_at SET DEFAULT localtimestamp;

ALTER TABLE ds_template_field
    ALTER COLUMN created_at TYPE timestamp without time zone USING created_at::timestamp,
    ALTER COLUMN updated_at TYPE timestamp without time zone USING updated_at::timestamp,
    ALTER COLUMN created_at SET DEFAULT localtimestamp,
    ALTER COLUMN updated_at SET DEFAULT localtimestamp;

ALTER TABLE ds_rule_config
    ALTER COLUMN created_at TYPE timestamp without time zone USING created_at::timestamp,
    ALTER COLUMN updated_at TYPE timestamp without time zone USING updated_at::timestamp,
    ALTER COLUMN created_at SET DEFAULT localtimestamp,
    ALTER COLUMN updated_at SET DEFAULT localtimestamp;
