package com.dataspec.reverseimport;

import com.dataspec.reverseimport.entity.DatabaseMetadataCacheEntry;
import com.dataspec.reverseimport.mapper.DatabaseMetadataCacheMapper;
import com.dataspec.reverseimport.repository.impl.DatabaseMetadataCacheRepositoryImpl;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * metadata cache repository 测试，覆盖并发写入同一唯一键时的幂等更新兜底。
 */
class DatabaseMetadataCacheRepositoryImplTest {

    @Test
    void upsert_recoversFromConcurrentInsertConflictByUpdatingExistingEntry() {
        DatabaseMetadataCacheMapper mapper = mock(DatabaseMetadataCacheMapper.class);
        DatabaseMetadataCacheRepositoryImpl repository = new DatabaseMetadataCacheRepositoryImpl(mapper);
        DatabaseMetadataCacheEntry entry = entry(null, "new-fingerprint");
        DatabaseMetadataCacheEntry existing = entry(7L, "old-fingerprint");
        existing.setFirstSeenAt(LocalDateTime.parse("2026-07-01T10:00:00"));
        existing.setCreatedAt(LocalDateTime.parse("2026-07-01T10:00:00"));
        when(mapper.selectOne(any())).thenReturn(null, existing);
        when(mapper.insert(any(DatabaseMetadataCacheEntry.class))).thenThrow(new DuplicateKeyException("duplicate metadata cache key"));

        repository.upsert(entry);

        assertThat(entry.getId()).isEqualTo(7L);
        assertThat(entry.getFirstSeenAt()).isEqualTo(existing.getFirstSeenAt());
        assertThat(entry.getCreatedAt()).isEqualTo(existing.getCreatedAt());
        verify(mapper).updateById(entry);
    }

    @Test
    void expire_updatesExistingEntryByUniqueCacheKeyWhenIdIsMissing() {
        DatabaseMetadataCacheMapper mapper = mock(DatabaseMetadataCacheMapper.class);
        DatabaseMetadataCacheRepositoryImpl repository = new DatabaseMetadataCacheRepositoryImpl(mapper);
        DatabaseMetadataCacheEntry entry = entry(null, "new-fingerprint");
        DatabaseMetadataCacheEntry existing = entry(7L, "old-fingerprint");
        existing.setFirstSeenAt(LocalDateTime.parse("2026-07-01T10:00:00"));
        existing.setCreatedAt(LocalDateTime.parse("2026-07-01T10:00:00"));
        when(mapper.selectOne(any())).thenReturn(existing);

        repository.expire(entry);

        assertThat(entry.getId()).isEqualTo(7L);
        assertThat(entry.getFirstSeenAt()).isEqualTo(existing.getFirstSeenAt());
        assertThat(entry.getCreatedAt()).isEqualTo(existing.getCreatedAt());
        verify(mapper).updateById(entry);
    }

    private DatabaseMetadataCacheEntry entry(Long id, String fingerprint) {
        DatabaseMetadataCacheEntry entry = new DatabaseMetadataCacheEntry();
        entry.setId(id);
        entry.setProjectId(1L);
        entry.setSourceScopeHash("scope");
        entry.setSchemaName("PUBLIC");
        entry.setTableName("USER_ORDER");
        entry.setTableFingerprint(fingerprint);
        entry.setMetadataJson("{}");
        entry.setIsDeleted(false);
        return entry;
    }
}
