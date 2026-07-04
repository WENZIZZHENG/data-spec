-- ============================================================
-- DataSpec V24: 项目标准质量门禁
-- ============================================================

CREATE TABLE IF NOT EXISTS ds_standard_quality_gate (
    id                          bigserial       PRIMARY KEY,
    project_id                  bigint          NOT NULL,
    enabled                     boolean         NOT NULL DEFAULT false,
    min_coverage                integer,
    min_average_field_score     integer,
    max_error_issues            integer,
    max_new_unmanaged_fields    integer,
    required_sensitive_marking  boolean         NOT NULL DEFAULT true,
    config_json                 text,
    created_at                  timestamp without time zone NOT NULL DEFAULT localtimestamp,
    updated_at                  timestamp without time zone NOT NULL DEFAULT localtimestamp,
    is_deleted                  boolean         NOT NULL DEFAULT false
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_standard_quality_gate_project
    ON ds_standard_quality_gate(project_id)
    WHERE is_deleted = false;

COMMENT ON TABLE ds_standard_quality_gate IS '项目级标准质量门禁配置';
COMMENT ON COLUMN ds_standard_quality_gate.enabled IS '是否启用质量门禁显式检查';
COMMENT ON COLUMN ds_standard_quality_gate.min_coverage IS '最低字段覆盖率百分比,0-100';
COMMENT ON COLUMN ds_standard_quality_gate.min_average_field_score IS '最低字段质量均分,0-100';
COMMENT ON COLUMN ds_standard_quality_gate.max_error_issues IS '允许的 ERROR 级问题数量上限';
COMMENT ON COLUMN ds_standard_quality_gate.max_new_unmanaged_fields IS '允许的未纳管字段数量上限';
COMMENT ON COLUMN ds_standard_quality_gate.required_sensitive_marking IS '是否要求疑似敏感字段必须标注';
COMMENT ON COLUMN ds_standard_quality_gate.config_json IS '预留扩展配置 JSON,不得保存密码、token、JDBC URL 或业务数据行';
