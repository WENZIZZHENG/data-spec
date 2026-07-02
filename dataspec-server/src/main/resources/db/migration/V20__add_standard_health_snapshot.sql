-- ============================================================
-- DataSpec V20: 标准健康快照
-- ============================================================

CREATE TABLE IF NOT EXISTS ds_standard_health_snapshot (
    id                         bigserial       PRIMARY KEY,
    project_id                 bigint          NOT NULL,
    captured_at                timestamp without time zone NOT NULL DEFAULT localtimestamp,
    source                     varchar(30)     NOT NULL DEFAULT 'MANUAL',
    average_quality_score      integer         NOT NULL DEFAULT 0,
    low_quality_field_count    integer         NOT NULL DEFAULT 0,
    total_field_count          integer         NOT NULL DEFAULT 0,
    coverage_status            varchar(30)     NOT NULL DEFAULT 'not_collected',
    coverage_rate              numeric(5,1),
    unmanaged_field_count      integer         NOT NULL DEFAULT 0,
    missing_comment_count      integer         NOT NULL DEFAULT 0,
    possible_duplicate_count   integer         NOT NULL DEFAULT 0,
    rule_issue_count           integer         NOT NULL DEFAULT 0,
    rule_exemption_count       integer         NOT NULL DEFAULT 0,
    ai_feedback_signal_count   integer         NOT NULL DEFAULT 0,
    pending_candidate_count    integer         NOT NULL DEFAULT 0,
    adopted_candidate_count    integer         NOT NULL DEFAULT 0,
    fixed_sql_available_count  integer         NOT NULL DEFAULT 0,
    top_actions_json           text,
    payload_json               text,
    created_at                 timestamp without time zone NOT NULL DEFAULT localtimestamp,
    updated_at                 timestamp without time zone NOT NULL DEFAULT localtimestamp,
    is_deleted                 boolean         NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_standard_health_snapshot_project_time
    ON ds_standard_health_snapshot(project_id, captured_at DESC)
    WHERE is_deleted = false;

COMMENT ON TABLE ds_standard_health_snapshot IS '项目级标准健康快照';
COMMENT ON COLUMN ds_standard_health_snapshot.coverage_status IS '覆盖率采集状态: collected/not_collected';
COMMENT ON COLUMN ds_standard_health_snapshot.top_actions_json IS 'AI 可读 Top actions JSON,不包含业务数据行';
COMMENT ON COLUMN ds_standard_health_snapshot.payload_json IS '健康快照补充 payload,必须经过敏感信息脱敏';
