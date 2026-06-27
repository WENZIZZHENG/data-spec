package com.dataspec.dbpreset.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库直连非敏感连接预设。
 */
@Data
@TableName("ds_database_connection_preset")
public class DatabaseConnectionPreset {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属项目 */
    private Long projectId;

    /** 预设别名 */
    private String name;

    /** 数据库类型，如 postgresql/mysql */
    private String databaseType;

    /** 主机地址，不包含用户名、密码或连接串 */
    private String host;

    /** 端口 */
    private Integer port;

    /** 数据库名 */
    private String databaseName;

    /** schema 名称 */
    private String schemaName;

    /** 默认表选择，内部用 JSON 数组持久化，API 返回 tableNames */
    @JsonIgnore
    private String tableNamesJson;

    @TableField(exist = false)
    private List<String> tableNames = new ArrayList<>();

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean isDeleted;
}
