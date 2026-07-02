ALTER TABLE ds_field
    ADD COLUMN IF NOT EXISTS replacement_field_id bigint,
    ADD COLUMN IF NOT EXISTS replacement_reason text;

CREATE INDEX IF NOT EXISTS idx_field_replacement
    ON ds_field(replacement_field_id)
    WHERE is_deleted = false;

COMMENT ON COLUMN ds_field.status IS '字段生命周期状态: draft/enabled/deprecated/disabled';
COMMENT ON COLUMN ds_field.replacement_field_id IS '废弃、停用或草稿字段的推荐替代字段 ID，同项目 ds_field.id';
COMMENT ON COLUMN ds_field.replacement_reason IS '字段生命周期替代说明、迁移建议或历史兼容原因';
