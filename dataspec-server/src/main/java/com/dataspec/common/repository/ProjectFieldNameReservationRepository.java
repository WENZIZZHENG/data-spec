package com.dataspec.common.repository;

/**
 * 项目字段名预留的持久化端口。
 */
public interface ProjectFieldNameReservationRepository {

    /** 在当前事务中串行化同一项目、同一字段名的字段或候选写入。 */
    void lock(Long projectId, String fieldName);

    /**
     * 检查项目字段名是否被 active 候选占用。
     *
     * @param excludedCandidateId 从候选采纳为字段时允许排除当前候选；直接创建字段时传 null
     */
    boolean existsActiveCandidate(Long projectId, String fieldName, Long excludedCandidateId);
}
