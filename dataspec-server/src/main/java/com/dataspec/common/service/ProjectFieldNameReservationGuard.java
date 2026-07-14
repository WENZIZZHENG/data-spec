package com.dataspec.common.service;

import com.dataspec.common.exception.BizException;
import com.dataspec.common.repository.ProjectFieldNameReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 统一标准字段写入与 active 候选之间的项目字段名预留边界。
 *
 * <p>调用方必须处于写事务中。批量入口先按字段名稳定排序并获取全部锁，再检查候选占用，
 * 避免不同批次以相反顺序取锁造成死锁，也避免字段与候选跨表 TOCTOU。</p>
 */
@Service
@RequiredArgsConstructor
public class ProjectFieldNameReservationGuard {

    private final ProjectFieldNameReservationRepository repository;

    /** 为单个字段写入预留名称；候选采纳可排除当前候选 ID。 */
    public void reserve(Long projectId, String fieldName, Long excludedCandidateId) {
        repository.lock(projectId, fieldName);
        rejectActiveCandidate(projectId, fieldName, excludedCandidateId);
    }

    /**
     * 为批量字段创建按稳定顺序预留全部名称。
     *
     * <p>该方法只检查 active 候选占用；调用方取得锁后还必须重新查询这些名称对应的标准字段，
     * 不能继续依据加锁前快照决定插入。</p>
     */
    public void reserveAll(Long projectId, Collection<String> fieldNames) {
        List<String> orderedNames = fieldNames == null
                ? List.of()
                : fieldNames.stream()
                        .filter(Objects::nonNull)
                        .filter(name -> !name.isBlank())
                        .distinct()
                        .sorted()
                        .toList();
        orderedNames.forEach(name -> repository.lock(projectId, name));
        orderedNames.forEach(name -> rejectActiveCandidate(projectId, name, null));
    }

    private void rejectActiveCandidate(Long projectId, String fieldName, Long excludedCandidateId) {
        if (repository.existsActiveCandidate(projectId, fieldName, excludedCandidateId)) {
            throw new BizException("同名候选仍待处理，请先在标准候选 Inbox 中采纳或合并: " + fieldName);
        }
    }
}
