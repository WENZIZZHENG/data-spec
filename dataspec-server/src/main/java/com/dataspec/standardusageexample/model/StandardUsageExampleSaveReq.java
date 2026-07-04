package com.dataspec.standardusageexample.model;

import lombok.Data;

/**
 * 标准使用示例保存请求。scope/exampleType/status 由服务层归一化。
 */
@Data
public class StandardUsageExampleSaveReq {
    private Long projectId;
    private Long fieldId;
    private String ruleCode;
    private Long templateId;
    private String scope;
    private String exampleType;
    private String input;
    private String expectedOutput;
    private String antiPattern;
    private String reason;
    private String tags;
    private Integer priority;
    private String status;
}
