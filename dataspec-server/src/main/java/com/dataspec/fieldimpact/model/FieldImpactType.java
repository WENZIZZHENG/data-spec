package com.dataspec.fieldimpact.model;

/**
 * 字段影响来源类型。
 */
public enum FieldImpactType {
    /** 表模板引用。 */
    TEMPLATE,
    /** 数据库反向导入来源引用。 */
    IMPORT_SOURCE,
    /** 历史 SQL 检查记录中的疑似引用。 */
    SQL_CHECK,
    /** 标准快照或 AI Context 中的版本化引用。 */
    STANDARD_SNAPSHOT,
    /** 字段关联代码集引用。 */
    CODE_SET,
    /** 业务仓库 SQL、迁移、模型或配置文件中的字段引用摘要。 */
    CODE_REFERENCE
}
