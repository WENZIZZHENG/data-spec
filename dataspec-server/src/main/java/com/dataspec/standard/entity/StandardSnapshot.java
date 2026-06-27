package com.dataspec.standard.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目级标准版本快照。
 */
@Data
@TableName("ds_standard_snapshot")
public class StandardSnapshot {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    /** 用户定义的标准版本号，如 v2026.06.24 */
    private String version;

    /** 快照名称，便于用户识别版本用途 */
    private String name;

    private String description;

    /** payloadJson 的 SHA-256 hash */
    private String snapshotHash;

    /** 字段、枚举和规则的确定性 JSON 快照 */
    private String payloadJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean isDeleted;
}
