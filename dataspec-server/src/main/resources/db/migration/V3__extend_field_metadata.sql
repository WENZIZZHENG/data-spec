-- ============================================================
-- DataSpec V3: 扩展个人版标准字段元数据
-- ============================================================

ALTER TABLE ds_field
    ADD COLUMN IF NOT EXISTS aliases varchar(500),
    ADD COLUMN IF NOT EXISTS category varchar(100),
    ADD COLUMN IF NOT EXISTS code_set_id bigint,
    ADD COLUMN IF NOT EXISTS sensitive boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS status varchar(20) NOT NULL DEFAULT 'enabled',
    ADD COLUMN IF NOT EXISTS example_value varchar(500);

CREATE INDEX IF NOT EXISTS idx_field_status ON ds_field(project_id, status) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_field_code_set ON ds_field(code_set_id) WHERE is_deleted = false;

COMMENT ON COLUMN ds_field.aliases IS '字段别名，逗号分隔，用于自然语言和历史字段名匹配';
COMMENT ON COLUMN ds_field.category IS '字段分类，如 contact、money、audit';
COMMENT ON COLUMN ds_field.code_set_id IS '关联枚举/代码集 ID，对应 ds_enum_dict.id';
COMMENT ON COLUMN ds_field.sensitive IS '是否敏感字段';
COMMENT ON COLUMN ds_field.status IS '字段状态: enabled/disabled/deprecated';
COMMENT ON COLUMN ds_field.example_value IS '字段示例值';
