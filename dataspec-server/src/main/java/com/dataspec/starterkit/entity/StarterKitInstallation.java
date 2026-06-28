package com.dataspec.starterkit.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 领域 Starter Kit 安装摘要。
 */
@Data
@TableName("ds_starter_kit_installation")
public class StarterKitInstallation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private String kitKey;

    private String kitVersion;

    private String kitName;

    private String createdCountsJson;

    private String skippedCountsJson;

    private String warningsJson;

    private String operatorName;

    private LocalDateTime appliedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean isDeleted;
}
