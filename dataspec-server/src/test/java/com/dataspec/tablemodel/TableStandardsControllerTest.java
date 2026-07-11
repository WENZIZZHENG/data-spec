package com.dataspec.tablemodel;

import com.dataspec.aicontext.model.AiContextScopeOptions;
import com.dataspec.common.exception.BizException;
import com.dataspec.tablemodel.controller.TableStandardsController;
import com.dataspec.tablemodel.service.TableStandardsContextProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 表结构标准只读接口测试，确保 CLI/MCP 依赖的 `/api/table-standards` 契约可用。
 */
class TableStandardsControllerTest {

    @Test
    void getTableStandardsUsesBusinessObjectScopeAndReturnsJson() throws Exception {
        RecordingProvider provider = new RecordingProvider();
        TableStandardsController controller = new TableStandardsController(provider, new ObjectMapper());

        var response = controller.getTableStandards(7L, null, "order", 10);

        assertEquals(200, response.getCode());
        assertEquals("dataspec-table-standards", response.getData().path("kind").asText());
        assertEquals(7L, provider.projectId);
        assertEquals("business-object", provider.options.scope());
        assertEquals("business-object:order", provider.options.query());
        assertEquals(10, provider.options.limit());
    }

    @Test
    void getTableStandardsUsesTemplateScope() throws Exception {
        RecordingProvider provider = new RecordingProvider();
        TableStandardsController controller = new TableStandardsController(provider, new ObjectMapper());

        controller.getTableStandards(7L, 12L, null, null);

        assertEquals("table-template", provider.options.scope());
        assertEquals("template-id:12", provider.options.query());
    }

    @Test
    void getTableStandardsRejectsConflictingScopes() {
        TableStandardsController controller = new TableStandardsController(new RecordingProvider(), new ObjectMapper());

        BizException error = assertThrows(BizException.class,
                () -> controller.getTableStandards(7L, 12L, "order", null));

        assertEquals("templateId 与 businessObject 只能二选一", error.getMessage());
    }

    private static final class RecordingProvider implements TableStandardsContextProvider {
        private Long projectId;
        private AiContextScopeOptions options;

        @Override
        public String generateTableStandardsJson(Long projectId) {
            return generateTableStandardsJson(projectId, AiContextScopeOptions.full());
        }

        @Override
        public String generateTableStandardsJson(Long projectId, AiContextScopeOptions options) {
            this.projectId = projectId;
            this.options = options;
            return """
                    {
                      "kind": "dataspec-table-standards",
                      "schemaVersion": 1,
                      "projectId": 7,
                      "businessObjects": [],
                      "templates": [],
                      "relations": [],
                      "summary": {},
                      "safety": {"readOnly": true, "writesProject": false},
                      "nextActions": ["Inspect structure standards before generating DDL."]
                    }
                    """;
        }

        @Override
        public String generateTableStandardsMarkdown(Long projectId) {
            return "";
        }
    }
}
