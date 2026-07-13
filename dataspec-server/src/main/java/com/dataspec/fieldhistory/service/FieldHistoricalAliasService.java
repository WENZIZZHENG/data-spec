package com.dataspec.fieldhistory.service;

import com.dataspec.field.entity.Field;
import com.dataspec.fieldhistory.model.FieldHistoricalAlias;

import java.util.List;
import java.util.Map;

/**
 * 从已有字段变更快照派生请求级历史名称索引。
 */
public interface FieldHistoricalAliasService {

    /**
     * 读取当前项目字段的可审计历史值。
     *
     * @param projectId     当前项目 ID；不得跨项目读取日志
     * @param currentFields 当前项目仍存在的字段，用于排除现行名称并忽略已删除对象
     * @return 按字段 ID 分组的历史值；没有可靠历史时返回空 map
     */
    Map<Long, List<FieldHistoricalAlias>> load(Long projectId, List<Field> currentFields);
}
