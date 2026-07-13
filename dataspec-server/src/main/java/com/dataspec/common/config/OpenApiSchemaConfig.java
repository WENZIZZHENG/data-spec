package com.dataspec.common.config;

import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 补充 Springdoc 无法直接保留的引用属性说明。
 */
@Configuration
public class OpenApiSchemaConfig {

    private static final String FIELD_SEARCH_RESULT = "FieldSearchResult";
    private static final String SUMMARY_DESCRIPTION = "匹配数量、过滤条件和确定性查询证据摘要。";
    private static final String PAGE_DESCRIPTION = "服务端分页元数据；legacy limit-only 调用为空以保持原有语义。";

    /**
     * 将字段搜索结果中的对象引用转换为带属性说明的 allOf 引用。
     *
     * <p>OpenAPI 3.0 的 Reference Object 不支持 description sibling，直接在 record
     * 属性上声明同类型 allOf 又会触发 Springdoc 递归，因此在文档生成完成后做局部规范化。</p>
     *
     * @return 只调整字段搜索结果引用属性的 OpenAPI customizer
     */
    @Bean
    public OpenApiCustomizer fieldSearchResultPropertyDescriptions() {
        return openApi -> {
            if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
                return;
            }
            Schema<?> resultSchema = openApi.getComponents().getSchemas().get(FIELD_SEARCH_RESULT);
            describeReferenceProperty(resultSchema, "summary", SUMMARY_DESCRIPTION, false);
            describeReferenceProperty(resultSchema, "page", PAGE_DESCRIPTION, true);
        };
    }

    private static void describeReferenceProperty(
            Schema<?> owner,
            String propertyName,
            String description,
            boolean nullable
    ) {
        if (owner == null || owner.getProperties() == null) {
            return;
        }
        Schema<?> property = owner.getProperties().get(propertyName);
        if (property == null || property.get$ref() == null) {
            return;
        }

        ComposedSchema describedProperty = new ComposedSchema();
        describedProperty.setDescription(description);
        describedProperty.setNullable(nullable);
        describedProperty.addAllOfItem(new Schema<>().$ref(property.get$ref()));
        owner.addProperty(propertyName, describedProperty);
    }
}
