package com.dataspec.reverseimport.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据库直连反向导入请求。密码只在本次请求内使用，不落库、不写日志。
 */
@Data
public class DatabaseConnectionReq {

    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    @NotBlank(message = "数据库类型不能为空")
    private String databaseType;

    @NotBlank(message = "主机不能为空")
    private String host;

    private Integer port;

    @NotBlank(message = "数据库名不能为空")
    private String databaseName;

    private String schemaName;

    @NotBlank(message = "用户名不能为空")
    private String username;

    private String password;

    private List<String> tableNames = new ArrayList<>();
}
