package com.dataspec.standardcandidate.repository;

import com.dataspec.standardcandidate.entity.StandardCandidate;

import java.util.Optional;

/**
 * 命名证据候选的持久化端口。
 *
 * <p>完整事实键固定为 projectId、candidateName、sourceType 和 sourceRef；实现必须与数据库唯一索引一致。</p>
 */
public interface TokenEvidenceCandidateRepository {

    /** 按完整事实键查询未删除候选，覆盖所有候选状态。 */
    Optional<StandardCandidate> findByFactKey(Long projectId, String candidateName, String sourceType, String sourceRef);

    /** 查询项目内同名 active 候选，用于阻止不同来源事实重复占用 Inbox。 */
    Optional<StandardCandidate> findActiveByName(Long projectId, String candidateName);

    /**
     * 在数据库唯一约束下尝试插入；事实已存在时返回 0，不抛出重复键异常。
     *
     * @return 1 表示插入成功，0 表示并发或重试命中既有事实
     */
    int insertIfAbsent(StandardCandidate candidate);
}
