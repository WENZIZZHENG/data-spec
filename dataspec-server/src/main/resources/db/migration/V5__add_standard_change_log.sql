-- ============================================================
-- 标准变更记录
-- ============================================================

CREATE TABLE IF NOT EXISTS ds_standard_change_log (
    id              bigserial       PRIMARY KEY,
    project_id      bigint          NOT NULL,
    target_type     varchar(50)     NOT NULL,
    target_id       bigint          NOT NULL,
    action          varchar(30)     NOT NULL,
    before_json     text,
    after_json      text,
    changed_at      timestamp with time zone NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_change_log_project_time
    ON ds_standard_change_log(project_id, changed_at DESC);

CREATE INDEX IF NOT EXISTS idx_change_log_target
    ON ds_standard_change_log(project_id, target_type, target_id, changed_at DESC);

COMMENT ON TABLE ds_standard_change_log IS '标准变更记录';
COMMENT ON COLUMN ds_standard_change_log.target_type IS '目标类型: field/enum_dict/enum_value/rule_config';
COMMENT ON COLUMN ds_standard_change_log.action IS '动作: create/update/delete/toggle';
COMMENT ON COLUMN ds_standard_change_log.before_json IS '变更前 JSON 快照';
COMMENT ON COLUMN ds_standard_change_log.after_json IS '变更后 JSON 快照';
