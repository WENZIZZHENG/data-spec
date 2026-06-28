-- ============================================================
-- DataSpec V16: 标准候选 Inbox
-- ============================================================

CREATE TABLE IF NOT EXISTS ds_standard_candidate (
    id                bigserial       PRIMARY KEY,
    project_id        bigint          NOT NULL,
    candidate_name    varchar(100)    NOT NULL,
    display_name      varchar(100),
    data_type         varchar(50)     NOT NULL,
    comment           text,
    source_type       varchar(50)     NOT NULL,
    source_ref        varchar(300),
    evidence_json     text,
    confidence        integer         NOT NULL DEFAULT 50,
    status            varchar(30)     NOT NULL DEFAULT 'PENDING',
    target_field_id   bigint,
    decision_reason   text,
    decided_at        timestamp without time zone,
    created_at        timestamp without time zone NOT NULL DEFAULT localtimestamp,
    updated_at        timestamp without time zone NOT NULL DEFAULT localtimestamp,
    is_deleted        boolean         NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_standard_candidate_project_status
    ON ds_standard_candidate(project_id, status, created_at DESC)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_standard_candidate_project_source
    ON ds_standard_candidate(project_id, source_type, created_at DESC)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_standard_candidate_project_name
    ON ds_standard_candidate(project_id, candidate_name)
    WHERE is_deleted = false;

COMMENT ON TABLE ds_standard_candidate IS '标准候选 Inbox 记录';
COMMENT ON COLUMN ds_standard_candidate.candidate_name IS '候选标准字段名';
COMMENT ON COLUMN ds_standard_candidate.source_type IS '候选来源,如 MANUAL/COVERAGE/REVERSE_IMPORT/AI_FEEDBACK';
COMMENT ON COLUMN ds_standard_candidate.evidence_json IS '候选证据 JSON,必须经过敏感信息脱敏';
COMMENT ON COLUMN ds_standard_candidate.status IS '候选状态:PENDING/ACCEPTED/MERGED/IGNORED/POSTPONED';
COMMENT ON COLUMN ds_standard_candidate.target_field_id IS '接受或合并后的目标标准字段 ID';
