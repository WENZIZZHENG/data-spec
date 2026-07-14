package com.dataspec.common.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 项目字段名预留 Mapper。
 *
 * <p>标准字段和 active 标准候选共享同一项目字段名命名空间；所有相关写入口必须先获取同一事务锁，
 * 再检查另一个表中的占用状态。</p>
 */
public interface ProjectFieldNameReservationMapper {

    /**
     * 获取当前事务内的项目字段名锁。
     *
     * @param projectId 项目 ID，作为 advisory lock seed
     * @param fieldName 已校验的字段名或候选名
     * @return 固定为 1；锁在事务结束时释放
     */
    @Select("""
            SELECT 1
            FROM pg_advisory_xact_lock(hashtextextended(#{fieldName}, #{projectId}))
            """)
    Integer lock(
            @Param("projectId") Long projectId,
            @Param("fieldName") String fieldName);

    /** 查询项目内是否已有同名 PENDING 或 POSTPONED 候选。 */
    @Select("""
            SELECT EXISTS (
                SELECT 1
                FROM ds_standard_candidate
                WHERE project_id = #{projectId}
                  AND candidate_name = #{fieldName}
                  AND status IN ('PENDING', 'POSTPONED')
                  AND is_deleted = false
            )
            """)
    boolean existsActiveCandidate(
            @Param("projectId") Long projectId,
            @Param("fieldName") String fieldName);

    /** 查询除当前采纳候选外，项目内是否还有同名 active 候选。 */
    @Select("""
            SELECT EXISTS (
                SELECT 1
                FROM ds_standard_candidate
                WHERE project_id = #{projectId}
                  AND candidate_name = #{fieldName}
                  AND id <> #{excludedCandidateId}
                  AND status IN ('PENDING', 'POSTPONED')
                  AND is_deleted = false
            )
            """)
    boolean existsOtherActiveCandidate(
            @Param("projectId") Long projectId,
            @Param("fieldName") String fieldName,
            @Param("excludedCandidateId") Long excludedCandidateId);
}
