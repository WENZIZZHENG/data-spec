package com.dataspec.reverseimport;

import com.dataspec.reverseimport.entity.DatabaseMetadataCacheEntry;
import com.dataspec.reverseimport.model.DatabaseConnectionReq;
import com.dataspec.reverseimport.model.DatabaseMetadataColumnChange;
import com.dataspec.reverseimport.model.DatabaseMetadataCacheMode;
import com.dataspec.reverseimport.model.DatabaseSchemaColumn;
import com.dataspec.reverseimport.model.DatabaseSchemaDump;
import com.dataspec.reverseimport.model.DatabaseSchemaIndex;
import com.dataspec.reverseimport.model.DatabaseSchemaSource;
import com.dataspec.reverseimport.model.DatabaseSchemaTable;
import com.dataspec.reverseimport.model.ReverseImportFieldChange;
import com.dataspec.reverseimport.repository.DatabaseMetadataCacheRepository;
import com.dataspec.reverseimport.service.impl.DatabaseMetadataCacheServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 数据库 metadata cache 服务测试，确保缓存只保存 schema-only 结构信息并能稳定生成变更指纹。
 */
class DatabaseMetadataCacheServiceImplTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-06T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void resolveDump_autoMissStoresSanitizedSnapshotAndFingerprint() {
        InMemoryDatabaseMetadataCacheRepository repository = new InMemoryDatabaseMetadataCacheRepository();
        DatabaseMetadataCacheServiceImpl service = newService(repository);
        DatabaseConnectionReq req = connectionReq();
        req.setPassword("top-secret");

        DatabaseSchemaDump result = service.resolveDump(req, () -> dumpWithColumns(
                "USER_ORDER",
                column("ID", "BIGINT", false, null, "主键"),
                column("API_TOKEN", "VARCHAR(100)", true, "password=top-secret", "jdbc:postgresql://localhost/demo Bearer token123")));

        assertThat(result.getMetadataCache().isCacheHit()).isFalse();
        assertThat(result.getMetadataCache().isStale()).isFalse();
        assertThat(result.getMetadataCache().getRefreshMode()).isEqualTo(DatabaseMetadataCacheMode.AUTO.name());
        assertThat(result.getMetadataCache().getMetadataFingerprint()).hasSize(64);
        DatabaseMetadataCacheEntry cached = repository.singleEntry();
        assertThat(cached.getTableFingerprint()).hasSize(64);
        assertThat(cached.getMetadataJson()).doesNotContain("top-secret", "jdbc:postgresql://localhost/demo", "token123");
    }

    @Test
    void resolveDump_autoHitUsesFreshCacheWithoutOpeningConnection() {
        InMemoryDatabaseMetadataCacheRepository repository = new InMemoryDatabaseMetadataCacheRepository();
        DatabaseMetadataCacheServiceImpl service = newService(repository);
        DatabaseConnectionReq req = connectionReq();
        service.resolveDump(req, () -> dumpWithColumns(
                "USER_ORDER",
                column("ID", "BIGINT", false, null, "主键")));

        DatabaseSchemaDump result = service.resolveDump(req, failingLoader());

        assertThat(result.getMetadataCache().isCacheHit()).isTrue();
        assertThat(result.getMetadataCache().getRefreshMode()).isEqualTo(DatabaseMetadataCacheMode.AUTO.name());
        assertThat(result.getTables()).hasSize(1);
        assertThat(result.getTables().get(0).getColumns()).extracting(DatabaseSchemaColumn::getColumnName)
                .containsExactly("ID");
    }

    @Test
    void resolveDump_refreshBuildsFieldChangeSummary() {
        InMemoryDatabaseMetadataCacheRepository repository = new InMemoryDatabaseMetadataCacheRepository();
        DatabaseMetadataCacheServiceImpl service = newService(repository);
        DatabaseConnectionReq req = connectionReq();
        service.resolveDump(req, () -> dumpWithColumns(
                "USER_ORDER",
                column("ID", "BIGINT", false, null, "主键"),
                column("PHONE", "VARCHAR(20)", true, null, "手机号")));

        req.setMetadataCacheMode(DatabaseMetadataCacheMode.REFRESH.name());
        DatabaseSchemaDump result = service.resolveDump(req, () -> dumpWithColumns(
                "USER_ORDER",
                column("ID", "VARCHAR(32)", false, null, "主键"),
                column("USER_NAME", "VARCHAR(50)", true, null, "用户名")));

        assertThat(result.getMetadataCache().isCacheHit()).isFalse();
        assertThat(result.getMetadataCache().getRefreshMode()).isEqualTo(DatabaseMetadataCacheMode.REFRESH.name());
        assertThat(result.getMetadataCache().getChangeSummary().isChanged()).isTrue();
        assertThat(result.getMetadataCache().getChangeSummary().getAddedColumnCount()).isEqualTo(1);
        assertThat(result.getMetadataCache().getChangeSummary().getRemovedColumnCount()).isEqualTo(1);
        assertThat(result.getMetadataCache().getChangeSummary().getChangedColumnCount()).isEqualTo(1);
        assertThat(result.getMetadataCache().getChangeSummary().getTables().get(0).getAddedColumns())
                .containsExactly("USER_NAME");
        assertThat(result.getMetadataCache().getChangeSummary().getTables().get(0).getRemovedColumns())
                .containsExactly("PHONE");
    }

    @Test
    void resolveDump_refreshReportsIndexMembershipAsColumnChange() {
        InMemoryDatabaseMetadataCacheRepository repository = new InMemoryDatabaseMetadataCacheRepository();
        DatabaseMetadataCacheServiceImpl service = newService(repository);
        DatabaseConnectionReq req = connectionReq();
        service.resolveDump(req, () -> dumpWithIndex(
                "USER_ORDER",
                "ID",
                column("ID", "BIGINT", false, null, "主键"),
                column("USER_NAME", "VARCHAR(50)", true, null, "用户名")));

        req.setMetadataCacheMode(DatabaseMetadataCacheMode.REFRESH.name());
        DatabaseSchemaDump result = service.resolveDump(req, () -> dumpWithIndex(
                "USER_ORDER",
                "USER_NAME",
                column("ID", "BIGINT", false, null, "主键"),
                column("USER_NAME", "VARCHAR(50)", true, null, "用户名")));

        assertThat(result.getMetadataCache().getChangeSummary().getChangedColumnCount()).isEqualTo(2);
        assertThat(result.getMetadataCache().getChangeSummary().getTables().get(0).getChangedColumns())
                .extracting(DatabaseMetadataColumnChange::getColumnName)
                .containsExactlyInAnyOrder("ID", "USER_NAME");
        assertThat(result.getMetadataCache().getChangeSummary().getTables().get(0).getChangedColumns())
                .flatExtracting(DatabaseMetadataColumnChange::getChanges)
                .extracting(ReverseImportFieldChange::getProperty)
                .containsOnly("indexMembership");
    }

    @Test
    void resolveDump_bypassDoesNotReadOrWriteCache() {
        InMemoryDatabaseMetadataCacheRepository repository = new InMemoryDatabaseMetadataCacheRepository();
        DatabaseMetadataCacheServiceImpl service = newService(repository);
        DatabaseConnectionReq req = connectionReq();
        req.setMetadataCacheMode(DatabaseMetadataCacheMode.BYPASS.name());

        DatabaseSchemaDump result = service.resolveDump(req, () -> dumpWithColumns(
                "USER_ORDER",
                column("ID", "BIGINT", false, null, "主键")));

        assertThat(result.getMetadataCache().isCacheHit()).isFalse();
        assertThat(result.getMetadataCache().getRefreshMode()).isEqualTo(DatabaseMetadataCacheMode.BYPASS.name());
        assertThat(result.getMetadataCache().getExpiresAt()).isNull();
        assertThat(repository.findCount).isZero();
        assertThat(repository.entries).isEmpty();
    }

    @Test
    void resolveDump_refreshExpiresRemovedTableCacheEntries() {
        InMemoryDatabaseMetadataCacheRepository repository = new InMemoryDatabaseMetadataCacheRepository();
        DatabaseMetadataCacheServiceImpl service = newService(repository);
        DatabaseConnectionReq req = connectionReq();
        req.setTableNames(List.of("USER_ORDER", "ACCOUNT"));
        service.resolveDump(req, () -> dumpWithTables(
                tableWithColumns("USER_ORDER", column("ID", "BIGINT", false, null, "主键")),
                tableWithColumns("ACCOUNT", column("ID", "BIGINT", false, null, "主键"))));

        req.setMetadataCacheMode(DatabaseMetadataCacheMode.REFRESH.name());
        DatabaseSchemaDump refreshed = service.resolveDump(req, () -> dumpWithTables(
                tableWithColumns("USER_ORDER", column("ID", "BIGINT", false, null, "主键"))));

        assertThat(refreshed.getMetadataCache().getChangeSummary().getRemovedTableCount()).isEqualTo(1);
        DatabaseMetadataCacheEntry removed = repository.entry("ACCOUNT");
        assertThat(removed.getExpiresAt()).isBeforeOrEqualTo(FIXED_CLOCK.instant().atOffset(ZoneOffset.UTC).toLocalDateTime());

        req.setMetadataCacheMode(DatabaseMetadataCacheMode.AUTO.name());
        req.setTableNames(List.of("ACCOUNT"));
        var summary = service.summarize(req, req.getTableNames());

        assertThat(summary.isCacheHit()).isFalse();
        assertThat(summary.isStale()).isTrue();
    }

    @Test
    void summarize_reportsMissWhenAnyRequestedTableIsMissingFromCache() {
        InMemoryDatabaseMetadataCacheRepository repository = new InMemoryDatabaseMetadataCacheRepository();
        DatabaseMetadataCacheServiceImpl service = newService(repository);
        DatabaseConnectionReq req = connectionReq();
        service.resolveDump(req, () -> dumpWithColumns(
                "USER_ORDER",
                column("ID", "BIGINT", false, null, "主键")));

        var summary = service.summarize(req, List.of("USER_ORDER", "ACCOUNT"));

        assertThat(summary.isCacheHit()).isFalse();
        assertThat(summary.isStale()).isFalse();
        assertThat(summary.getMetadataFingerprint()).hasSize(64);
    }

    private DatabaseMetadataCacheServiceImpl newService(DatabaseMetadataCacheRepository repository) {
        return new DatabaseMetadataCacheServiceImpl(repository, new ObjectMapper(), FIXED_CLOCK);
    }

    private DatabaseConnectionReq connectionReq() {
        DatabaseConnectionReq req = new DatabaseConnectionReq();
        req.setProjectId(1L);
        req.setDatabaseType("postgresql");
        req.setHost("localhost");
        req.setPort(5432);
        req.setDatabaseName("demo");
        req.setSchemaName("PUBLIC");
        req.setUsername("sa");
        req.setTableNames(List.of("USER_ORDER"));
        return req;
    }

    private DatabaseSchemaDump dumpWithColumns(String tableName, DatabaseSchemaColumn... columns) {
        return dumpWithTables(tableWithColumns(tableName, columns));
    }

    private DatabaseSchemaDump dumpWithTables(DatabaseSchemaTable... tables) {
        DatabaseSchemaDump dump = new DatabaseSchemaDump();
        dump.setProjectId(1L);
        dump.setDatabaseType("POSTGRESQL");
        dump.setDatabaseName("demo");
        dump.setSchemaName("PUBLIC");
        dump.setGeneratedAt("2026-07-06T10:00:00Z");
        DatabaseSchemaSource source = new DatabaseSchemaSource();
        source.setDatabaseProductName("PostgreSQL");
        source.setDatabaseProductVersion("16.0 password=top-secret");
        source.setSelectedTableNames(List.of(tables).stream().map(DatabaseSchemaTable::getTableName).toList());
        dump.setSource(source);
        dump.setTables(List.of(tables));
        return dump;
    }

    private DatabaseSchemaTable tableWithColumns(String tableName, DatabaseSchemaColumn... columns) {
        DatabaseSchemaTable table = new DatabaseSchemaTable();
        table.setSchemaName("PUBLIC");
        table.setTableName(tableName);
        table.setTableType("TABLE");
        table.setColumns(List.of(columns));
        DatabaseSchemaIndex index = new DatabaseSchemaIndex();
        index.setSchemaName("PUBLIC");
        index.setTableName(tableName);
        index.setIndexName("IDX_" + tableName + "_ID");
        index.setColumnName("ID");
        index.setNonUnique(false);
        index.setOrdinalPosition(1);
        table.setIndexes(List.of(index));
        return table;
    }

    private DatabaseSchemaDump dumpWithIndex(String tableName, String indexColumnName, DatabaseSchemaColumn... columns) {
        DatabaseSchemaDump dump = dumpWithColumns(tableName, columns);
        dump.getTables().get(0).setIndexes(List.of(index(tableName, indexColumnName)));
        return dump;
    }

    private DatabaseSchemaIndex index(String tableName, String columnName) {
        DatabaseSchemaIndex index = new DatabaseSchemaIndex();
        index.setSchemaName("PUBLIC");
        index.setTableName(tableName);
        index.setIndexName("IDX_" + tableName + "_" + columnName);
        index.setColumnName(columnName);
        index.setNonUnique(false);
        index.setOrdinalPosition(1);
        return index;
    }

    private DatabaseSchemaColumn column(String name, String dataType, boolean nullable, String defaultValue, String comment) {
        DatabaseSchemaColumn column = new DatabaseSchemaColumn();
        column.setColumnName(name);
        column.setDataType(dataType);
        column.setNullable(nullable);
        column.setDefaultValue(defaultValue);
        column.setComment(comment);
        column.setOrdinalPosition(name.equals("ID") ? 1 : 2);
        return column;
    }

    private Supplier<DatabaseSchemaDump> failingLoader() {
        return () -> {
            throw new AssertionError("缓存命中时不应打开 JDBC 连接");
        };
    }

    private static class InMemoryDatabaseMetadataCacheRepository implements DatabaseMetadataCacheRepository {

        private final Map<String, DatabaseMetadataCacheEntry> entries = new LinkedHashMap<>();
        private int findCount;

        @Override
        public Optional<DatabaseMetadataCacheEntry> findActive(Long projectId,
                                                               String sourceScopeHash,
                                                               String schemaName,
                                                               String tableName) {
            findCount++;
            return Optional.ofNullable(entries.get(key(projectId, sourceScopeHash, schemaName, tableName)));
        }

        @Override
        public void upsert(DatabaseMetadataCacheEntry entry) {
            entries.put(key(entry.getProjectId(), entry.getSourceScopeHash(), entry.getSchemaName(), entry.getTableName()), entry);
        }

        @Override
        public void expire(DatabaseMetadataCacheEntry entry) {
            entries.put(key(entry.getProjectId(), entry.getSourceScopeHash(), entry.getSchemaName(), entry.getTableName()), entry);
        }

        private DatabaseMetadataCacheEntry singleEntry() {
            assertThat(entries).hasSize(1);
            return entries.values().iterator().next();
        }

        private DatabaseMetadataCacheEntry entry(String tableName) {
            String matching = entries.keySet().stream()
                    .filter(key -> key.endsWith("|" + tableName))
                    .findFirst()
                    .orElseThrow();
            return entries.get(matching);
        }

        private String key(Long projectId, String sourceScopeHash, String schemaName, String tableName) {
            return projectId + "|" + sourceScopeHash + "|" + schemaName + "|" + tableName;
        }
    }
}
