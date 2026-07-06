package com.dataspec.syntheticexample.model;

import java.util.List;
import java.util.Map;

/**
 * 可复用于 DDL preview 的合成输入，描述目标表和期望字段集合。
 *
 * @param id 稳定输入 id。
 * @param tableName 建议的 snake_case 表名。
 * @param businessObject 业务对象描述。
 * @param expectedFieldNames DDL preview 应优先使用的标准字段名。
 * @param generationParams 生成该输入时使用的关键参数。
 */
public record SyntheticDdlPreviewInput(
        String id,
        String tableName,
        String businessObject,
        List<String> expectedFieldNames,
        Map<String, Object> generationParams
) {
}
