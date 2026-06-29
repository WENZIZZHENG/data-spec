CREATE TABLE IF NOT EXISTS ds_business_glossary (
    id bigserial PRIMARY KEY,
    project_id bigint NOT NULL,
    term varchar(120) NOT NULL,
    synonyms varchar(1000),
    root_terms varchar(1000),
    abbreviations varchar(500),
    disabled_terms varchar(1000),
    canonical_field_id bigint,
    scope_type varchar(30) NOT NULL DEFAULT 'GLOBAL',
    scope_value varchar(120),
    example_fields varchar(1000),
    description varchar(1000),
    status varchar(20) NOT NULL DEFAULT 'enabled',
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_business_glossary_active_term
    ON ds_business_glossary(project_id, lower(term))
    WHERE is_deleted = false AND status = 'enabled';

CREATE INDEX IF NOT EXISTS idx_business_glossary_project_status
    ON ds_business_glossary(project_id, status)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_business_glossary_canonical_field
    ON ds_business_glossary(project_id, canonical_field_id)
    WHERE is_deleted = false AND canonical_field_id IS NOT NULL;

COMMENT ON TABLE ds_business_glossary IS '项目级业务术语表与同义词词根库';
COMMENT ON COLUMN ds_business_glossary.term IS '主术语，例如会员、订单费用';
COMMENT ON COLUMN ds_business_glossary.synonyms IS '同义词，逗号分隔';
COMMENT ON COLUMN ds_business_glossary.root_terms IS '英文词根，逗号分隔';
COMMENT ON COLUMN ds_business_glossary.abbreviations IS '拼音、历史缩写或英文缩写，逗号分隔';
COMMENT ON COLUMN ds_business_glossary.disabled_terms IS '禁用或不推荐术语，逗号分隔';
COMMENT ON COLUMN ds_business_glossary.canonical_field_id IS '推荐 canonical 标准字段 ID';
COMMENT ON COLUMN ds_business_glossary.scope_type IS '适用范围类型：GLOBAL/CATEGORY/DOMAIN/TAG';
COMMENT ON COLUMN ds_business_glossary.scope_value IS '适用范围值';
COMMENT ON COLUMN ds_business_glossary.example_fields IS '示例字段名，逗号分隔';
