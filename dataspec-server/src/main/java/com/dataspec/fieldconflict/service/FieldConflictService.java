package com.dataspec.fieldconflict.service;

import com.dataspec.field.entity.Field;
import com.dataspec.fieldconflict.model.FieldConflictReport;

import java.util.List;

public interface FieldConflictService {
    /**
     * 基于项目完整字段库生成只读冲突报告。
     */
    FieldConflictReport report(Long projectId);

    /**
     * 基于调用方已经裁剪过的字段集合生成只读冲突报告。
     *
     * <p>AI Context 等按需导出场景必须使用该重载，避免把 scope 外字段写入导出内容。</p>
     */
    FieldConflictReport report(Long projectId, List<Field> fields);
}
