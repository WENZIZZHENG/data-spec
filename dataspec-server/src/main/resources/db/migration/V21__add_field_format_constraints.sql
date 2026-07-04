ALTER TABLE ds_field
    ADD COLUMN IF NOT EXISTS format_type varchar(64),
    ADD COLUMN IF NOT EXISTS format_pattern varchar(512),
    ADD COLUMN IF NOT EXISTS format_unit varchar(64),
    ADD COLUMN IF NOT EXISTS format_precision varchar(64),
    ADD COLUMN IF NOT EXISTS format_timezone varchar(64),
    ADD COLUMN IF NOT EXISTS format_null_policy varchar(64),
    ADD COLUMN IF NOT EXISTS valid_examples_json text,
    ADD COLUMN IF NOT EXISTS invalid_examples_json text,
    ADD COLUMN IF NOT EXISTS format_notes text;

COMMENT ON COLUMN ds_field.format_type IS '字段值格式类型，如 money/mobile/email/timestamp/json/status';
COMMENT ON COLUMN ds_field.format_pattern IS '字段值格式正则或轻量模式说明';
COMMENT ON COLUMN ds_field.format_unit IS '字段值单位，如 cent/yuan/ms/UTC';
COMMENT ON COLUMN ds_field.format_precision IS '字段值精度说明，如 scale=2/millisecond/6dp';
COMMENT ON COLUMN ds_field.format_timezone IS '时间类字段时区说明';
COMMENT ON COLUMN ds_field.format_null_policy IS '字段值空值策略说明，不改变 nullable 数据库约束';
COMMENT ON COLUMN ds_field.valid_examples_json IS '字段值正例 JSON 字符串数组';
COMMENT ON COLUMN ds_field.invalid_examples_json IS '字段值反例 JSON 字符串数组';
COMMENT ON COLUMN ds_field.format_notes IS '字段值格式补充说明';
