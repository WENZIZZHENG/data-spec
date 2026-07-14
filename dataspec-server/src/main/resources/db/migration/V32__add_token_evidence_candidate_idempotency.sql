-- ============================================================
-- DataSpec V32: 命名证据候选幂等约束
-- ============================================================

CREATE UNIQUE INDEX IF NOT EXISTS ux_standard_candidate_token_evidence_fact
    ON ds_standard_candidate(project_id, candidate_name, source_type, source_ref)
    WHERE is_deleted = false
      AND source_type = 'TOKEN_EVIDENCE'
      AND source_ref IS NOT NULL;

COMMENT ON INDEX ux_standard_candidate_token_evidence_fact IS
    '命名证据候选事实唯一键；保证同一项目、候选名、来源类型和来源引用只入箱一次';
