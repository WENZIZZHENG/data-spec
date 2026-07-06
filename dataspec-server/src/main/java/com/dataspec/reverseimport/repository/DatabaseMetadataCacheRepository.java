package com.dataspec.reverseimport.repository;

import com.dataspec.reverseimport.entity.DatabaseMetadataCacheEntry;

import java.util.Optional;

/**
 * 数据库 metadata cache 持久化端口，按项目和脱敏来源边界读写表级结构缓存。
 */
public interface DatabaseMetadataCacheRepository {

    /**
     * 查找未逻辑删除的表级 metadata cache。
     *
     * @param projectId 当前项目 ID
     * @param sourceScopeHash 脱敏连接来源 hash
     * @param schemaName schema 名；MySQL 场景可能为空
     * @param tableName 表名
     * @return 命中的缓存记录
     */
    Optional<DatabaseMetadataCacheEntry> findActive(Long projectId, String sourceScopeHash, String schemaName, String tableName);

    /**
     * 按项目、来源、schema、table 幂等写入缓存记录。
     *
     * @param entry schema-only metadata cache 记录
     */
    void upsert(DatabaseMetadataCacheEntry entry);

    /**
     * 将已确认不存在的表级缓存立即置为过期，避免后续 AUTO 在 TTL 内继续复用旧结构。
     *
     * @param entry 需要过期的旧缓存记录
     */
    void expire(DatabaseMetadataCacheEntry entry);
}
