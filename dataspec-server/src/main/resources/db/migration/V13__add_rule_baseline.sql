-- ============================================================
-- DataSpec V13: 项目规则基线套件元数据
-- ============================================================

CREATE TABLE IF NOT EXISTS ds_rule_baseline (
    id                  bigserial       PRIMARY KEY,
    project_id          bigint          NOT NULL,
    baseline_key        varchar(100)    NOT NULL,
    baseline_name       varchar(200)    NOT NULL,
    baseline_version    varchar(50)     NOT NULL,
    source              varchar(30)     NOT NULL,
    applied_at          timestamp with time zone NOT NULL DEFAULT now(),
    rules_json          text            NOT NULL,
    created_at          timestamp with time zone NOT NULL DEFAULT now(),
    updated_at          timestamp with time zone NOT NULL DEFAULT now(),
    is_deleted          boolean         NOT NULL DEFAULT false
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_rule_baseline_project
    ON ds_rule_baseline(project_id)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_rule_baseline_key_version
    ON ds_rule_baseline(baseline_key, baseline_version)
    WHERE is_deleted = false;

COMMENT ON TABLE ds_rule_baseline IS '项目规则基线套件元数据';
COMMENT ON COLUMN ds_rule_baseline.baseline_key IS '基线编码,如 personal_default/strict/legacy_compatible/custom';
COMMENT ON COLUMN ds_rule_baseline.baseline_version IS '基线版本';
COMMENT ON COLUMN ds_rule_baseline.source IS '基线来源,built_in/imported/inferred';
COMMENT ON COLUMN ds_rule_baseline.rules_json IS '应用或导入时的规则基线 JSON 包';
