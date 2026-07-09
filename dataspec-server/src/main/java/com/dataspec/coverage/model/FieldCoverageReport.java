package com.dataspec.coverage.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import com.dataspec.reverseimport.model.DatabaseMetadataCacheInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 项目字段标准覆盖率报告。
 */
@Data
@Schema(description = "项目字段标准覆盖率报告；可标记完整输入或采集作业 partial 输入边界。")
public class FieldCoverageReport {

    /** 输入完整性状态：COMPLETE/PARTIAL/CANCELLED/FAILED，用于提醒报告是否只覆盖成功采集表。 */
    @Schema(description = "输入完整性状态：COMPLETE/PARTIAL/CANCELLED/FAILED；PARTIAL 表示只统计成功采集的 schema-only 表。")
    private String inputStatus = "COMPLETE";

    /** 未纳入覆盖率计算的失败表数量；这些表不得视为已覆盖。 */
    @Schema(description = "未纳入覆盖率计算的失败表数量；这些表不得视为已覆盖。")
    private int failedTableCount;

    /** 未纳入覆盖率计算的跳过或未扫描表数量；这些表不得视为已覆盖。 */
    @Schema(description = "未纳入覆盖率计算的跳过或未扫描表数量；这些表不得视为已覆盖。")
    private int skippedTableCount;

    /** 针对 partial/cancelled/failed 输入的安全下一步，不包含凭据或源库业务数据。 */
    @Schema(description = "针对 partial/cancelled/failed 输入的安全下一步，不包含凭据、完整连接串或源库业务数据。")
    private List<String> nextActions = new ArrayList<>();

    private FieldCoverageSummary summary = new FieldCoverageSummary();
    private List<FieldCoverageTable> tables = new ArrayList<>();
    private List<UnmanagedFieldRanking> unmanagedRankings = new ArrayList<>();
    /** 数据库直连覆盖率报告关联的 metadata cache 证据；SQL 或 dump 输入场景可为空。 */
    private DatabaseMetadataCacheInfo metadataCache;
}
