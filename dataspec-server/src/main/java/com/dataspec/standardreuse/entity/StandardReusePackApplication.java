package com.dataspec.standardreuse.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 标准复用包应用摘要。
 *
 * <p>只保存目标项目、包摘要和漂移报告，不保存完整包 payload，避免历史记录长期携带不必要内容。</p>
 */
@Data
@TableName("ds_standard_reuse_pack_application")
public class StandardReusePackApplication {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 目标项目 ID。 */
    private Long projectId;

    /** 应用时引用的复用包 ID。 */
    private Long packId;

    /** 复用包 key 快照。 */
    private String packKey;

    /** 复用包名称快照。 */
    private String packName;

    /** 复用包版本快照。 */
    private String basePackVersion;

    /** 复用包内容 hash 快照。 */
    private String packageHash;

    /** 源项目 ID 快照。 */
    private Long sourceProjectId;

    /** 源项目名称快照。 */
    private String sourceProjectName;

    /** 本次应用创建的资产数量 JSON。 */
    private String createdCountsJson;

    /** 本次应用跳过的资产数量 JSON。 */
    private String skippedCountsJson;

    /** 本次应用后的漂移计数 JSON。 */
    private String driftCountsJson;

    /** 本次应用后的漂移报告 JSON，不包含 raw secret 或源库行值。 */
    private String driftReportJson;

    /** 操作者显示名。 */
    private String operatorName;

    /** 应用时间。 */
    private LocalDateTime appliedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean isDeleted;
}
