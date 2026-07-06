package com.dataspec.reverseimport.service;

import com.dataspec.reverseimport.model.DatabaseConnectionReq;
import com.dataspec.reverseimport.model.DatabaseMetadataCacheInfo;
import com.dataspec.reverseimport.model.DatabaseSchemaDump;

import java.util.List;
import java.util.function.Supplier;

/**
 * 数据库 metadata cache 服务，负责 schema-only 快照复用、刷新、fingerprint 和变更摘要。
 */
public interface DatabaseMetadataCacheService {

    /**
     * 根据请求缓存策略解析 schema dump；必要时调用 sourceLoader 读取源库。
     *
     * @param req 数据库直连请求，密码仅可传给 sourceLoader 当前请求使用
     * @param sourceLoader 源库 metadata 读取函数
     * @return 带 metadata cache 证据的 schema dump
     */
    DatabaseSchemaDump resolveDump(DatabaseConnectionReq req, Supplier<DatabaseSchemaDump> sourceLoader);

    /**
     * 汇总已缓存表的 cache 证据，不触发源库读取。
     *
     * @param req 数据库直连请求
     * @param tableNames 需要汇总的表名
     * @return metadata cache 摘要
     */
    DatabaseMetadataCacheInfo summarize(DatabaseConnectionReq req, List<String> tableNames);
}
