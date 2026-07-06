package com.dataspec.reverseimport.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据库 metadata cache 证据，描述结构快照的新鲜度、来源和 fingerprint。
 */
@Data
public class DatabaseMetadataCacheInfo {

    /** 本次选中表结构的聚合 fingerprint，供 AI 判断是否重跑反向导入或覆盖率。 */
    private String metadataFingerprint;

    /** true 表示本次结果完全来自新鲜缓存。 */
    private boolean cacheHit;

    /** true 表示曾发现缓存过期或缺失，需要重新读取源库。 */
    private boolean stale;

    /** 实际使用的刷新策略：AUTO/REFRESH/BYPASS。 */
    private String refreshMode = DatabaseMetadataCacheMode.AUTO.name();

    /** 本次结构快照最近一次读取源库 metadata 的时间。 */
    private String lastSeenAt;

    /** 当前缓存过期时间；为空表示本次未写入或未读取缓存。 */
    private String expiresAt;

    /** 源数据库产品和版本的脱敏摘要。 */
    private String sourceDatabaseVersion;

    /** 刷新时产生的结构变化摘要。 */
    private DatabaseMetadataChangeSummary changeSummary = new DatabaseMetadataChangeSummary();

    /** 面向用户和 AI 的安全下一步提示，不包含凭据。 */
    private List<String> nextActions = new ArrayList<>();
}
