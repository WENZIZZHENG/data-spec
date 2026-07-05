package com.dataspec.reverseimport.model;

import lombok.Data;

/**
 * 数据库 metadata 分页扫描进度，供前端和 AI 判断是否继续下一批。
 */
@Data
public class DatabaseMetadataScanProgress {

    /** 本轮扫描已处理的表数量。 */
    private int processedTableCount;

    /** 根据当前表列表估算的剩余表数量。 */
    private int remainingTableEstimate;

    /** 本次请求采用的分页大小。 */
    private int pageSize;

    /** true 表示仍有下一批可继续读取。 */
    private boolean hasMore;
}
