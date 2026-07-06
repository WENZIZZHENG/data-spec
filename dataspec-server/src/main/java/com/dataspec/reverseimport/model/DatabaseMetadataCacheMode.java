package com.dataspec.reverseimport.model;

/**
 * 数据库 metadata cache 读取策略。
 */
public enum DatabaseMetadataCacheMode {
    /** 默认策略：新鲜缓存可复用，缺失或过期时读取源库并更新缓存。 */
    AUTO,
    /** 强制读取源库并更新缓存，用于生成结构变更摘要。 */
    REFRESH,
    /** 完全绕过缓存，只服务当前请求，不读写缓存。 */
    BYPASS
}
