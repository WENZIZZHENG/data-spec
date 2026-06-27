-- ============================================================
-- DataSpec V11: 项目级规则误报豁免
-- ============================================================

CREATE TABLE IF NOT EXISTS ds_rule_exemption (
    id              bigserial       PRIMARY KEY,
    project_id      bigint          NOT NULL,
    rule_code       varchar(100)    NOT NULL,
    table_name      varchar(200),
    column_name     varchar(200),
    reason          text            NOT NULL,
    enabled         boolean         NOT NULL DEFAULT true,
    expires_at      timestamp with time zone,
    created_at      timestamp with time zone NOT NULL DEFAULT now(),
    updated_at      timestamp with time zone NOT NULL DEFAULT now(),
    is_deleted      boolean         NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_rule_exemption_project_rule
    ON ds_rule_exemption(project_id, rule_code)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_rule_exemption_project_enabled
    ON ds_rule_exemption(project_id, enabled, expires_at)
    WHERE is_deleted = false;

COMMENT ON TABLE ds_rule_exemption IS '项目级规则误报豁免';
COMMENT ON COLUMN ds_rule_exemption.rule_code IS '被豁免的规则编码';
COMMENT ON COLUMN ds_rule_exemption.table_name IS '豁免表名,为空表示不限表';
COMMENT ON COLUMN ds_rule_exemption.column_name IS '豁免字段名,为空表示不限字段';
COMMENT ON COLUMN ds_rule_exemption.reason IS '豁免原因,用于人工和 AI 判断该例外不是新标准';
COMMENT ON COLUMN ds_rule_exemption.expires_at IS '豁免过期时间,为空表示不过期';
