package com.dataspec.standardchange.model;

/**
 * 字段变更预览请求。字段集合与字段编辑表单保持一致，但预览接口只读不落库。
 */
public record FieldChangePreviewReq(
        Long projectId,
        String name,
        String displayName,
        String dataType,
        Integer length,
        Integer precisionVal,
        Integer scaleVal,
        Boolean nullable,
        String defaultValue,
        String comment,
        Long domainId,
        String tags,
        String aliases,
        String category,
        Long codeSetId,
        Boolean sensitive,
        String status,
        String exampleValue
) {
}
