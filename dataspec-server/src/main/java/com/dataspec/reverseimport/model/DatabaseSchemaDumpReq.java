package com.dataspec.reverseimport.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 以离线 schema dump 作为输入的分析请求。
 */
@Data
public class DatabaseSchemaDumpReq {

    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    @Valid
    @NotNull(message = "schema dump 不能为空")
    private DatabaseSchemaDump dump;
}
