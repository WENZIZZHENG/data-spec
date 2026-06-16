package com.dataspec.lint.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 规则执行上下文
 */
@Data
@Builder
public class RuleContext {

    /** 解析后的表定义列表 */
    private List<TableDef> tables;

    /** 项目 ID（用于查询项目级规则配置） */
    private Long projectId;

    /** 规则参数（从 RuleConfig.paramsJson 解析） */
    private Map<String, Object> ruleParams;
}
