ALTER TABLE ds_field
    ADD COLUMN IF NOT EXISTS localized_names_json text,
    ADD COLUMN IF NOT EXISTS preferred_english_name varchar(200),
    ADD COLUMN IF NOT EXISTS forbidden_translations_json text,
    ADD COLUMN IF NOT EXISTS translation_aliases_json text,
    ADD COLUMN IF NOT EXISTS translation_confidence varchar(20),
    ADD COLUMN IF NOT EXISTS translation_notes text,
    ADD COLUMN IF NOT EXISTS semantic_summary text;

ALTER TABLE ds_enum_value
    ADD COLUMN IF NOT EXISTS status varchar(20) NOT NULL DEFAULT 'enabled',
    ADD COLUMN IF NOT EXISTS aliases_json text,
    ADD COLUMN IF NOT EXISTS replacement_value varchar(200),
    ADD COLUMN IF NOT EXISTS valid_from date,
    ADD COLUMN IF NOT EXISTS valid_to date,
    ADD COLUMN IF NOT EXISTS source_evidence text,
    ADD COLUMN IF NOT EXISTS mapping_hints text,
    ADD COLUMN IF NOT EXISTS ai_usage_notes text;

CREATE TABLE IF NOT EXISTS ds_field_semantic_rule (
    id                  bigserial       PRIMARY KEY,
    project_id          bigint          NOT NULL,
    field_id            bigint          NOT NULL,
    source_field_id     bigint,
    rule_type           varchar(50)     NOT NULL,
    unit_conversion     text,
    aggregation_rule    text,
    time_granularity    varchar(50),
    source_of_truth     text,
    recommended_use     text,
    anti_patterns       text,
    evidence_refs_json  text,
    status              varchar(20)     NOT NULL DEFAULT 'enabled',
    created_at          timestamp with time zone NOT NULL DEFAULT now(),
    updated_at          timestamp with time zone NOT NULL DEFAULT now(),
    is_deleted          boolean         NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS ds_metric_definition (
    id                  bigserial       PRIMARY KEY,
    project_id          bigint          NOT NULL,
    metric_key          varchar(100)    NOT NULL,
    display_name        varchar(200)    NOT NULL,
    definition          text            NOT NULL,
    measure_fields_json text,
    dimension_fields_json text,
    filter_rule         text,
    aggregation_rule    text,
    time_grain          varchar(50),
    owner_notes         text,
    example_sql         text,
    evidence_refs_json  text,
    status              varchar(20)     NOT NULL DEFAULT 'enabled',
    created_at          timestamp with time zone NOT NULL DEFAULT now(),
    updated_at          timestamp with time zone NOT NULL DEFAULT now(),
    is_deleted          boolean         NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_field_semantic_rule_project
    ON ds_field_semantic_rule(project_id)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_field_semantic_rule_field
    ON ds_field_semantic_rule(field_id)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_field_semantic_rule_source
    ON ds_field_semantic_rule(source_field_id)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_field_semantic_rule_type
    ON ds_field_semantic_rule(project_id, rule_type)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_metric_definition_project
    ON ds_metric_definition(project_id)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_metric_definition_status
    ON ds_metric_definition(project_id, status)
    WHERE is_deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uk_metric_definition_project_key
    ON ds_metric_definition(project_id, metric_key)
    WHERE is_deleted = false;

COMMENT ON COLUMN ds_field.localized_names_json IS '字段本地化名称 JSON，如中文名、英文名或业务别名；不得包含凭据或业务数据行';
COMMENT ON COLUMN ds_field.preferred_english_name IS '推荐英文标准字段名或命名片段，用于 AI 命名建议和翻译辅助';
COMMENT ON COLUMN ds_field.forbidden_translations_json IS '禁用翻译数组 JSON，AI 命中后需要提示不要直接采用';
COMMENT ON COLUMN ds_field.translation_aliases_json IS '翻译别名数组 JSON，用于搜索、推荐和 AI Context 命名匹配';
COMMENT ON COLUMN ds_field.translation_confidence IS '命名翻译置信度，如 high、medium、low，仅作为人工维护提示';
COMMENT ON COLUMN ds_field.translation_notes IS '命名翻译说明、来源或边界；不得包含 token、JDBC URL、DSN、Authorization 或业务数据行';
COMMENT ON COLUMN ds_field.semantic_summary IS '字段语义摘要，说明单位、口径、source of truth 或常见误用；只做 AI guidance';

COMMENT ON COLUMN ds_enum_value.status IS '枚举值生命周期状态：enabled、deprecated、disabled 或 draft';
COMMENT ON COLUMN ds_enum_value.aliases_json IS '枚举值别名数组 JSON，用于 AI 识别历史值、展示值或外部系统映射';
COMMENT ON COLUMN ds_enum_value.replacement_value IS '废弃或停用枚举值的推荐替代值，仅作 guidance，不自动改写 SQL';
COMMENT ON COLUMN ds_enum_value.valid_from IS '枚举值有效期开始日期，可为空';
COMMENT ON COLUMN ds_enum_value.valid_to IS '枚举值有效期结束日期，可为空';
COMMENT ON COLUMN ds_enum_value.source_evidence IS '枚举值来源证据或维护说明；不得包含凭据或业务数据行';
COMMENT ON COLUMN ds_enum_value.mapping_hints IS '枚举值跨系统映射提示，如外部编码、展示名或兼容说明';
COMMENT ON COLUMN ds_enum_value.ai_usage_notes IS '枚举值 AI 使用说明；不得包含 token、密码、完整 JDBC URL、DSN 或业务数据行';

COMMENT ON TABLE ds_field_semantic_rule IS '字段语义规则，描述派生关系、单位换算、聚合口径、时间粒度和 source of truth guidance';
COMMENT ON COLUMN ds_field_semantic_rule.project_id IS '所属项目 ID，只能在项目授权范围内读写';
COMMENT ON COLUMN ds_field_semantic_rule.field_id IS '目标标准字段 ID，必须属于同一项目';
COMMENT ON COLUMN ds_field_semantic_rule.source_field_id IS '可选源字段 ID，用于 derivedFrom 或 source-of-truth 关系，必须属于同一项目';
COMMENT ON COLUMN ds_field_semantic_rule.rule_type IS '语义规则类型，如 DERIVED_FROM、UNIT_CONVERSION、AGGREGATION、TIME_GRAIN、SOURCE_OF_TRUTH、NAMING';
COMMENT ON COLUMN ds_field_semantic_rule.unit_conversion IS '单位换算说明，只做 guidance，不执行真实数据计算';
COMMENT ON COLUMN ds_field_semantic_rule.aggregation_rule IS '聚合口径说明，如 sum/count/distinct/ratio，不替代指标平台';
COMMENT ON COLUMN ds_field_semantic_rule.time_granularity IS '时间粒度说明，如 timestamp、date、day、month';
COMMENT ON COLUMN ds_field_semantic_rule.source_of_truth IS 'source of truth 或首选字段说明，用于 AI 避免口径混用';
COMMENT ON COLUMN ds_field_semantic_rule.recommended_use IS '推荐使用场景；不得包含真实业务数据行或凭据';
COMMENT ON COLUMN ds_field_semantic_rule.anti_patterns IS '常见误用、反例或禁用场景；不得包含 raw secret 或业务数据行';
COMMENT ON COLUMN ds_field_semantic_rule.evidence_refs_json IS '证据引用数组 JSON，可关联标准示例、决策记录或文档片段';
COMMENT ON COLUMN ds_field_semantic_rule.status IS '语义规则状态，enabled 表示默认进入知识卡和 AI Context';

COMMENT ON TABLE ds_metric_definition IS '轻量指标口径定义，用于把业务指标映射到标准字段、过滤条件、聚合方式和时间粒度';
COMMENT ON COLUMN ds_metric_definition.project_id IS '所属项目 ID，只能在项目授权范围内读写';
COMMENT ON COLUMN ds_metric_definition.metric_key IS '项目内唯一指标键，建议使用 snake_case，不含凭据或业务数据行';
COMMENT ON COLUMN ds_metric_definition.display_name IS '指标展示名称，如订单金额、支付成功率';
COMMENT ON COLUMN ds_metric_definition.definition IS '指标业务定义文本，说明统计边界和含义';
COMMENT ON COLUMN ds_metric_definition.measure_fields_json IS '度量字段引用数组 JSON，字段必须属于同一项目';
COMMENT ON COLUMN ds_metric_definition.dimension_fields_json IS '维度字段引用数组 JSON，字段必须属于同一项目';
COMMENT ON COLUMN ds_metric_definition.filter_rule IS '指标过滤口径说明，如状态、时间范围或软删除条件；不自动改写 SQL';
COMMENT ON COLUMN ds_metric_definition.aggregation_rule IS '指标聚合口径说明，如 sum/count/distinct/ratio';
COMMENT ON COLUMN ds_metric_definition.time_grain IS '指标默认时间粒度，如 day、week、month';
COMMENT ON COLUMN ds_metric_definition.owner_notes IS '维护者说明或取舍记录；不得包含凭据或业务数据行';
COMMENT ON COLUMN ds_metric_definition.example_sql IS '示例 SQL，仅作说明和 AI guidance，不会被执行';
COMMENT ON COLUMN ds_metric_definition.evidence_refs_json IS '证据引用数组 JSON，可关联字段、示例、决策记录或文档片段';
COMMENT ON COLUMN ds_metric_definition.status IS '指标口径状态，enabled 表示默认进入知识卡和 AI Context';
