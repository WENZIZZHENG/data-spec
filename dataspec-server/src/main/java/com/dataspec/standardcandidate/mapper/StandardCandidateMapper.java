package com.dataspec.standardcandidate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dataspec.standardcandidate.entity.StandardCandidate;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

/**
 * 标准候选 Mapper。
 */
public interface StandardCandidateMapper extends BaseMapper<StandardCandidate> {

    /**
     * 原子插入命名证据候选；数据库唯一索引负责跨线程和跨进程幂等。
     *
     * @param candidate 已完成脱敏、校验且 sourceType 为 TOKEN_EVIDENCE 的候选
     * @return 1 表示插入成功，0 表示完整事实键已存在
     */
    @Insert("""
            INSERT INTO ds_standard_candidate (
                project_id, candidate_name, display_name, data_type, comment,
                source_type, source_ref, evidence_json, confidence, status
            ) VALUES (
                #{candidate.projectId}, #{candidate.candidateName}, #{candidate.displayName},
                #{candidate.dataType}, #{candidate.comment}, #{candidate.sourceType},
                #{candidate.sourceRef}, #{candidate.evidenceJson}, #{candidate.confidence},
                #{candidate.status}
            )
            ON CONFLICT DO NOTHING
            """)
    @Options(useGeneratedKeys = true, keyProperty = "candidate.id", keyColumn = "id")
    int insertTokenEvidenceIfAbsent(@Param("candidate") StandardCandidate candidate);
}
