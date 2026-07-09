package com.dataspec.reverseimport.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 当前采集页可继续复用的 schema-only 部分结果。
 */
@Data
@Schema(description = "数据库 metadata 采集作业的 schema-only 部分结果；区分成功、失败和跳过表，供预览或覆盖率只使用成功表。")
public class DatabaseMetadataScanPartialResult {

    /** 成功读取到列/索引 metadata 的表结构；不包含业务数据行。 */
    @Schema(description = "成功读取到列和索引 metadata 的表结构；不包含业务数据行。")
    private List<DatabaseSchemaTable> successfulTables = new ArrayList<>();

    /** 成功读取 metadata 的表名，便于前端继续选择。 */
    @Schema(description = "成功读取 metadata 的表名，便于前端继续选择或生成覆盖率。")
    private List<String> successfulTableNames = new ArrayList<>();

    /** 失败表名；客户端不得静默导入这些表。 */
    @Schema(description = "读取失败的表名；客户端不得静默导入或覆盖这些表。")
    private List<String> failedTableNames = new ArrayList<>();

    /** 跳过表名；通常表示未在本页扫描或用户未选择。 */
    @Schema(description = "跳过的表名；通常表示未在本页扫描或用户未选择。")
    private List<String> skippedTableNames = new ArrayList<>();

    /** true 表示成功表足以生成反向导入预览，但只限 successfulTableNames。 */
    @Schema(description = "true 表示成功表足以生成反向导入预览，但只限 successfulTableNames。")
    private boolean completeForPreview;

    /** true 表示成功表足以生成覆盖率报告，但报告必须标记 partial 边界。 */
    @Schema(description = "true 表示成功表足以生成覆盖率报告，但报告必须标记 partial 边界。")
    private boolean completeForCoverage;

    /** true 表示整个扫描范围已完成且没有失败表。 */
    @Schema(description = "true 表示整个扫描范围已完成且没有失败表。")
    private boolean complete;
}
