package com.dataspec.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiSchemaConfigTest {

    @Test
    void fieldSearchResultPropertyDescriptions_wrapReferencesWithoutRecursion() {
        Schema<?> resultSchema = new ObjectSchema()
                .addProperty("summary", new Schema<>().$ref("#/components/schemas/FieldSearchSummary"))
                .addProperty("page", new Schema<>().$ref("#/components/schemas/FieldSearchPage"));
        OpenAPI openApi = new OpenAPI().components(new Components()
                .addSchemas("FieldSearchResult", resultSchema));

        new OpenApiSchemaConfig().fieldSearchResultPropertyDescriptions().customise(openApi);

        Schema<?> summary = resultSchema.getProperties().get("summary");
        Schema<?> page = resultSchema.getProperties().get("page");
        assertEquals("匹配数量、过滤条件和确定性查询证据摘要。", summary.getDescription());
        assertEquals("#/components/schemas/FieldSearchSummary", summary.getAllOf().getFirst().get$ref());
        assertFalse(summary.getNullable());
        assertEquals("服务端分页元数据；legacy limit-only 调用为空以保持原有语义。", page.getDescription());
        assertEquals("#/components/schemas/FieldSearchPage", page.getAllOf().getFirst().get$ref());
        assertTrue(page.getNullable());
    }
}
