package com.dataspec.security.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * API token 元数据。
 * <p>
 * 只保存 token hash，明文 token 由部署方一次性生成并自行保管。
 */
@Data
@TableName("ds_api_token")
public class ApiToken {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** token 名称，便于区分 CLI、MCP 或个人访问入口 */
    private String name;

    /** SHA-256 token hash */
    private String tokenHash;

    /** 变更日志中记录的操作者名称 */
    private String operatorName;

    /** 授权项目 ID，逗号分隔；* 表示全部项目 */
    private String projectIds;

    /** 是否启用 */
    private Boolean enabled;

    /** 最近一次认证成功时间，用于识别长期未使用 token */
    private LocalDateTime lastUsedAt;

    /** 停用时间；保留 enabled 是为了兼容现有鉴权判断 */
    private LocalDateTime disabledAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean isDeleted;
}
