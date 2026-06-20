package com.dataspec.reverseimport.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 确认导入数据库反向导入字段候选。
 */
@Data
public class DatabaseImportReq {

    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    @Valid
    private List<FieldCandidate> candidates = new ArrayList<>();

    /** 以下来源上下文字段仅用于追踪直连反向导入，不包含密码或完整连接串。 */
    private String databaseType;
    private String databaseName;
    private String schemaName;
    private List<String> tableNames = new ArrayList<>();
}
