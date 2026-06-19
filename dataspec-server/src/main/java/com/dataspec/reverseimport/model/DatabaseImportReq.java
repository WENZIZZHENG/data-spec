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
}
