package com.dataspec.project.model;

import com.dataspec.project.entity.Project;

/**
 * 演示项目初始化结果。
 */
public record DemoProjectResult(
        Project project,
        Long templateId,
        String sampleTableName,
        String badExampleSql,
        String goodExampleSql,
        boolean created
) {
}
