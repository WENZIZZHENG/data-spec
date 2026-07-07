ALTER TABLE ds_field
    ADD COLUMN IF NOT EXISTS preferred_use_cases text,
    ADD COLUMN IF NOT EXISTS avoid_when text,
    ADD COLUMN IF NOT EXISTS join_hints text,
    ADD COLUMN IF NOT EXISTS default_filters text,
    ADD COLUMN IF NOT EXISTS aggregation_hints text,
    ADD COLUMN IF NOT EXISTS replacement_guidance text,
    ADD COLUMN IF NOT EXISTS misuse_examples text;

COMMENT ON COLUMN ds_field.preferred_use_cases IS '字段推荐使用场景，说明适合用于哪些 SQL、指标、写入或 DDL 场景；不得包含凭据或业务数据行';
COMMENT ON COLUMN ds_field.avoid_when IS '字段禁用或需确认场景，AI 命中这些场景时不得直接采纳；不得包含密码、token、完整 JDBC URL、DSN 或私钥';
COMMENT ON COLUMN ds_field.join_hints IS '字段 Join 使用提示，如推荐关联键、关联方向或不适合 Join 的边界；只做只读指导';
COMMENT ON COLUMN ds_field.default_filters IS '字段默认过滤条件或统计口径提示，如状态、时间范围或软删除条件；不自动改写 SQL';
COMMENT ON COLUMN ds_field.aggregation_hints IS '字段聚合口径提示，如 sum/count/distinct/单位换算；不替代指标平台';
COMMENT ON COLUMN ds_field.replacement_guidance IS '字段在特定场景下的替代字段或迁移指导，与生命周期替代说明互补';
COMMENT ON COLUMN ds_field.misuse_examples IS '字段常见误用或反例说明，用于 AI 低置信提示；不得包含真实业务数据行或凭据';
