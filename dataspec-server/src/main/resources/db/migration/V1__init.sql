-- ============================================================
-- DataSpec 数据库初始化脚本 (PostgreSQL 17)
-- Flyway V1 基线:首次部署的完整 schema
-- ============================================================

-- 1. 项目空间
CREATE TABLE IF NOT EXISTS ds_project (
    id              bigserial       PRIMARY KEY,
    name            varchar(100)    NOT NULL,
    description     text,
    db_type         varchar(20)     NOT NULL DEFAULT 'postgresql',
    created_at      timestamp with time zone NOT NULL DEFAULT now(),
    updated_at      timestamp with time zone NOT NULL DEFAULT now(),
    is_deleted      boolean         NOT NULL DEFAULT false
);

-- 2. 标准字段库
CREATE TABLE IF NOT EXISTS ds_field (
    id              bigserial       PRIMARY KEY,
    project_id      bigint          NOT NULL,
    name            varchar(100)    NOT NULL,
    display_name    varchar(100),
    data_type       varchar(50)     NOT NULL,
    length          integer,
    precision_val   integer,
    scale_val       integer,
    nullable        boolean         NOT NULL DEFAULT true,
    default_value   varchar(200),
    comment         text,
    domain_id       bigint,
    tags            varchar(500),
    created_at      timestamp with time zone NOT NULL DEFAULT now(),
    updated_at      timestamp with time zone NOT NULL DEFAULT now(),
    is_deleted      boolean         NOT NULL DEFAULT false
);

-- 3. 数据域
CREATE TABLE IF NOT EXISTS ds_domain (
    id              bigserial       PRIMARY KEY,
    project_id      bigint          NOT NULL,
    name            varchar(100)    NOT NULL,
    code            varchar(50)     NOT NULL,
    description     text,
    created_at      timestamp with time zone NOT NULL DEFAULT now(),
    updated_at      timestamp with time zone NOT NULL DEFAULT now(),
    is_deleted      boolean         NOT NULL DEFAULT false
);

-- 4. 枚举字典
CREATE TABLE IF NOT EXISTS ds_enum_dict (
    id              bigserial       PRIMARY KEY,
    project_id      bigint          NOT NULL,
    name            varchar(100)    NOT NULL,
    code            varchar(50)     NOT NULL,
    description     text,
    value_type      varchar(20)     NOT NULL DEFAULT 'integer',
    created_at      timestamp with time zone NOT NULL DEFAULT now(),
    updated_at      timestamp with time zone NOT NULL DEFAULT now(),
    is_deleted      boolean         NOT NULL DEFAULT false
);

-- 5. 枚举值
CREATE TABLE IF NOT EXISTS ds_enum_value (
    id              bigserial       PRIMARY KEY,
    enum_id         bigint          NOT NULL,
    value           varchar(100)    NOT NULL,
    label           varchar(200)    NOT NULL,
    sort_order      integer         NOT NULL DEFAULT 0,
    created_at      timestamp with time zone NOT NULL DEFAULT now(),
    updated_at      timestamp with time zone NOT NULL DEFAULT now(),
    is_deleted      boolean         NOT NULL DEFAULT false
);

-- 6. 表模板
CREATE TABLE IF NOT EXISTS ds_template (
    id              bigserial       PRIMARY KEY,
    project_id      bigint          NOT NULL,
    name            varchar(100)    NOT NULL,
    description     text,
    table_prefix    varchar(50),
    created_at      timestamp with time zone NOT NULL DEFAULT now(),
    updated_at      timestamp with time zone NOT NULL DEFAULT now(),
    is_deleted      boolean         NOT NULL DEFAULT false
);

-- 7. 表模板字段
CREATE TABLE IF NOT EXISTS ds_template_field (
    id              bigserial       PRIMARY KEY,
    template_id     bigint          NOT NULL,
    field_id        bigint,
    name            varchar(100)    NOT NULL,
    data_type       varchar(50)     NOT NULL,
    nullable        boolean         NOT NULL DEFAULT true,
    default_value   varchar(200),
    comment         text,
    sort_order      integer         NOT NULL DEFAULT 0,
    is_required     boolean         NOT NULL DEFAULT false,
    created_at      timestamp with time zone NOT NULL DEFAULT now(),
    updated_at      timestamp with time zone NOT NULL DEFAULT now(),
    is_deleted      boolean         NOT NULL DEFAULT false
);

-- 8. 规则配置
CREATE TABLE IF NOT EXISTS ds_rule_config (
    id              bigserial       PRIMARY KEY,
    project_id      bigint          NOT NULL,
    rule_code       varchar(50)     NOT NULL,
    rule_name       varchar(100)    NOT NULL,
    severity        varchar(20)     NOT NULL DEFAULT 'warning',
    enabled         boolean         NOT NULL DEFAULT true,
    params_json     text,
    created_at      timestamp with time zone NOT NULL DEFAULT now(),
    updated_at      timestamp with time zone NOT NULL DEFAULT now(),
    is_deleted      boolean         NOT NULL DEFAULT false
);

-- ============================================================
-- 索引
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_field_project ON ds_field(project_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_field_name ON ds_field(name) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_domain_project ON ds_domain(project_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_enum_project ON ds_enum_dict(project_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_enum_value_enum ON ds_enum_value(enum_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_template_project ON ds_template(project_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_template_field_tpl ON ds_template_field(template_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_rule_config_project ON ds_rule_config(project_id) WHERE is_deleted = false;

-- ============================================================
-- 注释
-- ============================================================
COMMENT ON TABLE ds_project IS '项目空间';
COMMENT ON TABLE ds_field IS '标准字段库';
COMMENT ON TABLE ds_domain IS '数据域';
COMMENT ON TABLE ds_enum_dict IS '枚举字典';
COMMENT ON TABLE ds_enum_value IS '枚举值';
COMMENT ON TABLE ds_template IS '表模板';
COMMENT ON TABLE ds_template_field IS '表模板字段';
COMMENT ON TABLE ds_rule_config IS '规则配置';
