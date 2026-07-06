package com.dataspec.reverseimport.service.impl;

import com.dataspec.reverseimport.model.DatabaseConnectionReq;
import com.dataspec.reverseimport.model.DatabaseMetadataCacheInfo;
import com.dataspec.reverseimport.model.DatabaseMetadataCacheMode;
import com.dataspec.reverseimport.model.DatabaseSchemaDump;
import com.dataspec.reverseimport.service.DatabaseMetadataCacheService;

import java.util.List;
import java.util.function.Supplier;

/**
 * 测试和局部构造器使用的 no-op cache 服务，保持旧直连流程可不依赖持久化组件。
 */
final class NoopDatabaseMetadataCacheService implements DatabaseMetadataCacheService {

    static final NoopDatabaseMetadataCacheService INSTANCE = new NoopDatabaseMetadataCacheService();

    private NoopDatabaseMetadataCacheService() {
    }

    @Override
    public DatabaseSchemaDump resolveDump(DatabaseConnectionReq req, Supplier<DatabaseSchemaDump> sourceLoader) {
        DatabaseSchemaDump dump = sourceLoader.get();
        DatabaseMetadataCacheInfo info = new DatabaseMetadataCacheInfo();
        info.setRefreshMode(DatabaseMetadataCacheMode.BYPASS.name());
        dump.setMetadataCache(info);
        return dump;
    }

    @Override
    public DatabaseMetadataCacheInfo summarize(DatabaseConnectionReq req, List<String> tableNames) {
        DatabaseMetadataCacheInfo info = new DatabaseMetadataCacheInfo();
        info.setRefreshMode(DatabaseMetadataCacheMode.BYPASS.name());
        return info;
    }
}
