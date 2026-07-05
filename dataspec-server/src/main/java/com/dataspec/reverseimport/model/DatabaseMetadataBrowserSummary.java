package com.dataspec.reverseimport.model;

import lombok.Data;

/**
 * 数据库 metadata 浏览器的结构和标准分析汇总。
 */
@Data
public class DatabaseMetadataBrowserSummary {

    /** 本次浏览的表数量。 */
    private int tableCount;

    /** 本次浏览的字段数量。 */
    private int columnCount;

    /** 本次浏览的索引列 metadata 数量，复合索引按列计数。 */
    private int indexCount;

    /** 可作为反向导入候选的字段数量。 */
    private int candidateCount;

    /** 缺少表或字段注释的数量。 */
    private int missingCommentCount;

    /** 与现有标准字段命中但属性存在差异的字段数量。 */
    private int changedCount;

    /** 未纳入当前项目标准字段库的字段数量。 */
    private int unmanagedCount;
}
