package com.dataspec.projectbackup.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目备份恢复摘要。
 *
 * <p>只保存恢复结果和计数，不保存完整备份包，避免长期落库潜在敏感内容。</p>
 */
@Data
@TableName("ds_project_restore_record")
public class ProjectRestoreRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private String packageHash;
    private String sourceProjectName;
    private Long sourceProjectId;
    private Integer schemaVersion;
    private Boolean dryRun;
    private Boolean overwrite;
    private Integer createdCount;
    private Integer updatedCount;
    private Integer skippedCount;
    private Integer conflictCount;
    private Integer blockedCount;
    private Integer warningCount;
    private String summaryJson;
    private String operatorName;
    private LocalDateTime createdAt;
}
