CREATE TABLE IF NOT EXISTS ds_business_object_standard (
    id                  bigserial       PRIMARY KEY,
    project_id          bigint          NOT NULL,
    object_key          varchar(100)    NOT NULL,
    entity_name         varchar(200)    NOT NULL,
    table_pattern       varchar(200),
    template_id         bigint,
    required_fields_json text,
    optional_fields_json text,
    relations_json      text,
    foreign_key_hints_json text,
    audit_fields_json   text,
    common_pitfalls_json text,
    ai_usage_notes      text,
    context_export      boolean         NOT NULL DEFAULT true,
    status              varchar(20)     NOT NULL DEFAULT 'ENABLED',
    created_at          timestamp with time zone NOT NULL DEFAULT now(),
    updated_at          timestamp with time zone NOT NULL DEFAULT now(),
    is_deleted          boolean         NOT NULL DEFAULT false
);

ALTER TABLE ds_template
    ADD COLUMN IF NOT EXISTS business_object_id bigint,
    ADD COLUMN IF NOT EXISTS primary_key_json text,
    ADD COLUMN IF NOT EXISTS unique_keys_json text,
    ADD COLUMN IF NOT EXISTS indexes_json text,
    ADD COLUMN IF NOT EXISTS foreign_keys_json text,
    ADD COLUMN IF NOT EXISTS check_hints_json text,
    ADD COLUMN IF NOT EXISTS audit_policy_json text,
    ADD COLUMN IF NOT EXISTS soft_delete_policy_json text,
    ADD COLUMN IF NOT EXISTS dialect_notes_json text,
    ADD COLUMN IF NOT EXISTS ai_usage_notes text;

CREATE INDEX IF NOT EXISTS idx_business_object_project
    ON ds_business_object_standard(project_id)
    WHERE is_deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uk_business_object_project_key
    ON ds_business_object_standard(project_id, object_key)
    WHERE is_deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uk_business_object_project_entity
    ON ds_business_object_standard(project_id, entity_name)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_template_business_object
    ON ds_template(business_object_id)
    WHERE is_deleted = false;

COMMENT ON TABLE ds_business_object_standard IS '业务对象与表结构标准，用于描述业务实体、表模板依赖、关系提示和 AI 建表上下文';
COMMENT ON COLUMN ds_business_object_standard.project_id IS '所属项目 ID，只能在项目授权范围内读写';
COMMENT ON COLUMN ds_business_object_standard.object_key IS '项目内唯一业务对象键，建议使用 snake_case 或 kebab-case，不含凭据或业务数据行';
COMMENT ON COLUMN ds_business_object_standard.entity_name IS '业务对象展示名称，如订单、用户、支付记录，项目内唯一';
COMMENT ON COLUMN ds_business_object_standard.table_pattern IS '推荐表名模式或前缀提示，仅用于 DDL/AI guidance，不自动创建表';
COMMENT ON COLUMN ds_business_object_standard.template_id IS '关联表模板 ID，必须属于同一项目，可为空';
COMMENT ON COLUMN ds_business_object_standard.required_fields_json IS '必选字段标准引用或字段名数组 JSON，不得包含真实业务数据行或凭据';
COMMENT ON COLUMN ds_business_object_standard.optional_fields_json IS '可选字段标准引用或字段名数组 JSON，不得包含真实业务数据行或凭据';
COMMENT ON COLUMN ds_business_object_standard.relations_json IS '业务对象关系数组 JSON，包含 source/target/relationType/notes 等结构化提示，不保存 raw SQL';
COMMENT ON COLUMN ds_business_object_standard.foreign_key_hints_json IS '外键提示数组 JSON，只用于 preview/lint/AI guidance，不自动执行迁移';
COMMENT ON COLUMN ds_business_object_standard.audit_fields_json IS '审计字段提示 JSON，说明 created/updated/by 等字段约定';
COMMENT ON COLUMN ds_business_object_standard.common_pitfalls_json IS '常见反模式或误用说明 JSON，供 AI 生成 DDL/SQL 前避让';
COMMENT ON COLUMN ds_business_object_standard.ai_usage_notes IS 'AI 使用说明文本，不得包含 token、密码、完整 JDBC URL、DSN 或业务数据行';
COMMENT ON COLUMN ds_business_object_standard.context_export IS '是否默认导出到 AI Context table-standards.json';
COMMENT ON COLUMN ds_business_object_standard.status IS '业务对象状态，ENABLED 表示默认可被 DDL/AI Context 消费';

COMMENT ON COLUMN ds_template.business_object_id IS '可选关联业务对象标准 ID，必须属于同一项目';
COMMENT ON COLUMN ds_template.primary_key_json IS '主键标准 JSON，描述 constraintName 和 columns；只用于 DDL preview，不执行迁移';
COMMENT ON COLUMN ds_template.unique_keys_json IS '唯一键标准数组 JSON，每项描述 name 和 columns';
COMMENT ON COLUMN ds_template.indexes_json IS '索引标准数组 JSON，每项描述 name、columns、unique、method 等安全结构字段';
COMMENT ON COLUMN ds_template.foreign_keys_json IS '外键标准数组 JSON，每项描述 columns、targetTable、targetColumns、onDelete/onUpdate 等安全结构字段';
COMMENT ON COLUMN ds_template.check_hints_json IS 'CHECK 约束提示 JSON，默认只作为 lint/AI guidance，不拼接 raw SQL';
COMMENT ON COLUMN ds_template.audit_policy_json IS '审计字段策略 JSON，用于描述 created_at/updated_at/created_by/updated_by 等约定';
COMMENT ON COLUMN ds_template.soft_delete_policy_json IS '软删除策略 JSON，用于描述删除标记字段、默认过滤和恢复边界';
COMMENT ON COLUMN ds_template.dialect_notes_json IS '方言差异说明 JSON，用于提示 PostgreSQL/MySQL 等差异，不改变当前 PostgreSQL preview 边界';
COMMENT ON COLUMN ds_template.ai_usage_notes IS '表模板 AI 使用说明文本，不得包含凭据、连接串或业务数据行';
