package com.dataspec.projectbackup.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * 项目备份恢复确认请求。
 *
 * @param targetProjectId 目标项目 ID；为空时恢复到新项目。
 * @param overwrite 是否允许覆盖同 key 资产；默认为 false。
 * @param backupPackage 前序导出的脱敏备份包，不包含 token、数据库密码或源库业务数据行。
 * @param dryRunToken 预览恢复返回的 dry-run evidence；确认应用时必须原样带回。
 */
public record ProjectRestoreReq(
        @Schema(description = "目标项目 ID；为空时恢复到新项目。")
        Long targetProjectId,
        @Schema(description = "是否允许覆盖同 key 资产；默认为 false。")
        Boolean overwrite,
        @Schema(description = "前序导出的脱敏备份包，不包含 token、数据库密码或源库业务数据行。")
        @Valid @NotNull(message = "备份包不能为空") ProjectBackupPackage backupPackage,
        @Schema(description = "预览恢复返回的 dry-run evidence；确认应用时必须原样带回。")
        String dryRunToken
) {
    public ProjectRestoreReq(Long targetProjectId, Boolean overwrite, ProjectBackupPackage backupPackage) {
        this(targetProjectId, overwrite, backupPackage, null);
    }
}
