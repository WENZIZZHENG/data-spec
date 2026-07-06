package com.dataspec.reverseimport.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * metadata cache 刷新产生的结构变化汇总，供用户和 AI 判断是否需要重跑下游分析。
 */
@Data
public class DatabaseMetadataChangeSummary {

    /** true 表示本次刷新相对旧缓存存在结构变化。 */
    private boolean changed;

    /** 新增表数量。 */
    private int addedTableCount;

    /** 删除表数量。 */
    private int removedTableCount;

    /** 存在字段或索引属性变化的表数量。 */
    private int changedTableCount;

    /** 新增字段数量。 */
    private int addedColumnCount;

    /** 删除字段数量。 */
    private int removedColumnCount;

    /** 字段属性变化数量。 */
    private int changedColumnCount;

    /** 表级变化示例；大库场景应保持有界。 */
    private List<DatabaseMetadataTableChange> tables = new ArrayList<>();
}
