package com.dataspec.tablemodel.controller;

import com.dataspec.aicontext.model.AiContextScopeOptions;
import com.dataspec.common.exception.BizException;
import com.dataspec.common.result.R;
import com.dataspec.tablemodel.service.TableStandardsContextProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 表结构标准只读 API，给 CLI、MCP 和 AI 客户端读取与 AI Context 同构的 table standards 契约。
 */
@RestController
@RequestMapping("/api/table-standards")
@RequiredArgsConstructor
public class TableStandardsController {

    private static final String EXACT_TEMPLATE_ID_PREFIX = "template-id:";
    private static final String EXACT_BUSINESS_OBJECT_PREFIX = "business-object:";

    private final TableStandardsContextProvider tableStandardsContextProvider;
    private final ObjectMapper objectMapper;

    /**
     * 读取项目表结构标准。
     *
     * <p>该接口只返回 DataSpec 标准元数据和安全摘要，不连接业务数据库、不执行 DDL、不写项目状态。</p>
     */
    @GetMapping
    public R<JsonNode> getTableStandards(@RequestParam @NotNull Long projectId,
                                         @RequestParam(required = false) Long templateId,
                                         @RequestParam(required = false) String businessObject,
                                         @RequestParam(required = false) Integer limit) throws Exception {
        AiContextScopeOptions options = scopeOptions(templateId, businessObject, limit);
        return R.ok(objectMapper.readTree(tableStandardsContextProvider.generateTableStandardsJson(projectId, options)));
    }

    private AiContextScopeOptions scopeOptions(Long templateId, String businessObject, Integer limit) {
        if (templateId != null && businessObject != null && !businessObject.isBlank()) {
            throw new BizException("templateId 与 businessObject 只能二选一");
        }
        if (templateId != null) {
            return new AiContextScopeOptions("table-template", EXACT_TEMPLATE_ID_PREFIX + templateId, null, limit);
        }
        if (businessObject != null && !businessObject.isBlank()) {
            return new AiContextScopeOptions("business-object", EXACT_BUSINESS_OBJECT_PREFIX + businessObject, null, limit);
        }
        return new AiContextScopeOptions("all", null, null, limit);
    }
}
