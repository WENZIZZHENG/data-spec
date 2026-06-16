package com.dataspec.enumdict.service;

import com.dataspec.enumdict.entity.EnumDict;
import com.dataspec.enumdict.entity.EnumValue;

import java.util.List;

/**
 * 枚举字典服务接口
 */
public interface EnumDictService {
    List<EnumDict> listByProject(Long projectId);
    EnumDict getById(Long id);
    EnumDict create(EnumDict enumDict);
    EnumDict update(Long id, EnumDict enumDict);
    void delete(Long id);

    List<EnumValue> listValues(Long enumId);
    EnumValue createValue(EnumValue value);
    EnumValue updateValue(Long id, EnumValue value);
    void deleteValue(Long id);
}
