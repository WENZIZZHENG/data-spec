package com.dataspec.reverseimport.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dataspec.reverseimport.entity.DatabaseMetadataCacheEntry;
import com.dataspec.reverseimport.mapper.DatabaseMetadataCacheMapper;
import com.dataspec.reverseimport.repository.DatabaseMetadataCacheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 基于 MyBatis-Plus 的 metadata cache repository 实现。
 */
@Repository
@RequiredArgsConstructor
public class DatabaseMetadataCacheRepositoryImpl implements DatabaseMetadataCacheRepository {

    private final DatabaseMetadataCacheMapper mapper;

    @Override
    public Optional<DatabaseMetadataCacheEntry> findActive(Long projectId,
                                                           String sourceScopeHash,
                                                           String schemaName,
                                                           String tableName) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<DatabaseMetadataCacheEntry>()
                .eq(DatabaseMetadataCacheEntry::getProjectId, projectId)
                .eq(DatabaseMetadataCacheEntry::getSourceScopeHash, sourceScopeHash)
                .eq(DatabaseMetadataCacheEntry::getSchemaName, schemaName)
                .eq(DatabaseMetadataCacheEntry::getTableName, tableName)
                .last("LIMIT 1")));
    }

    @Override
    public void upsert(DatabaseMetadataCacheEntry entry) {
        findActive(entry.getProjectId(), entry.getSourceScopeHash(), entry.getSchemaName(), entry.getTableName())
                .ifPresent(existing -> attachExistingIdentity(entry, existing));
        if (entry.getId() == null) {
            try {
                mapper.insert(entry);
            } catch (DuplicateKeyException e) {
                // 并发刷新同一来源/表时可能同时 miss 后插入；唯一键冲突后回读并更新，保持 upsert 幂等语义。
                DatabaseMetadataCacheEntry existing = findActive(
                        entry.getProjectId(),
                        entry.getSourceScopeHash(),
                        entry.getSchemaName(),
                        entry.getTableName())
                        .orElseThrow(() -> e);
                attachExistingIdentity(entry, existing);
                mapper.updateById(entry);
            }
        } else {
            mapper.updateById(entry);
        }
    }

    @Override
    public void expire(DatabaseMetadataCacheEntry entry) {
        if (entry.getId() == null) {
            findActive(entry.getProjectId(), entry.getSourceScopeHash(), entry.getSchemaName(), entry.getTableName())
                    .ifPresent(existing -> attachExistingIdentity(entry, existing));
        }
        if (entry.getId() != null) {
            mapper.updateById(entry);
        }
    }

    private void attachExistingIdentity(DatabaseMetadataCacheEntry entry, DatabaseMetadataCacheEntry existing) {
        entry.setId(existing.getId());
        entry.setFirstSeenAt(existing.getFirstSeenAt());
        entry.setCreatedAt(existing.getCreatedAt());
    }
}
