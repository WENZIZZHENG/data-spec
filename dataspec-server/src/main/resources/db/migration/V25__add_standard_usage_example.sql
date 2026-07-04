-- ============================================================
-- DataSpec V25: 标准字段使用示例与反例库
-- ============================================================

CREATE TABLE IF NOT EXISTS ds_standard_usage_example (
    id               bigserial       PRIMARY KEY,
    project_id       bigint          NOT NULL,
    field_id         bigint,
    rule_code        varchar(100),
    template_id      bigint,
    scope            varchar(30)     NOT NULL,
    example_type     varchar(20)     NOT NULL,
    input            text            NOT NULL,
    expected_output  text,
    anti_pattern     text,
    reason           text            NOT NULL,
    tags             varchar(500),
    priority         integer         NOT NULL DEFAULT 50,
    status           varchar(20)     NOT NULL DEFAULT 'enabled',
    created_at       timestamp without time zone NOT NULL DEFAULT localtimestamp,
    updated_at       timestamp without time zone NOT NULL DEFAULT localtimestamp,
    is_deleted       boolean         NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_standard_usage_example_project_scope
    ON ds_standard_usage_example(project_id, scope, example_type, status)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_standard_usage_example_project_field
    ON ds_standard_usage_example(project_id, field_id)
    WHERE is_deleted = false;

COMMENT ON TABLE ds_standard_usage_example IS '项目级标准字段、规则和模板的 AI 使用正例/反例库';
COMMENT ON COLUMN ds_standard_usage_example.field_id IS 'FIELD scope 绑定的标准字段 ID';
COMMENT ON COLUMN ds_standard_usage_example.rule_code IS 'RULE scope 绑定的规则编码';
COMMENT ON COLUMN ds_standard_usage_example.template_id IS 'TEMPLATE scope 绑定的模板 ID';
COMMENT ON COLUMN ds_standard_usage_example.scope IS '适用范围: FIELD/RULE/TEMPLATE/GENERAL';
COMMENT ON COLUMN ds_standard_usage_example.example_type IS '示例类型: GOOD/BAD';
COMMENT ON COLUMN ds_standard_usage_example.input IS '输入或场景描述,不得保存真实业务数据或 secret';
COMMENT ON COLUMN ds_standard_usage_example.expected_output IS 'GOOD 示例的期望输出';
COMMENT ON COLUMN ds_standard_usage_example.anti_pattern IS 'BAD 示例的反模式片段';
COMMENT ON COLUMN ds_standard_usage_example.reason IS '示例/反例原因,供 AI 判断是否可模仿';
COMMENT ON COLUMN ds_standard_usage_example.tags IS '标签,逗号分隔';
COMMENT ON COLUMN ds_standard_usage_example.priority IS '导出优先级,0-100,数值越大越优先';
COMMENT ON COLUMN ds_standard_usage_example.status IS 'enabled/disabled';
