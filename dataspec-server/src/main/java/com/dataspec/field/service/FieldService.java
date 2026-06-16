package com.dataspec.field.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dataspec.field.entity.Field;

import java.util.List;

public interface FieldService {
    IPage<Field> page(Long projectId, int current, int size);
    List<Field> listByProject(Long projectId);
    Field getById(Long id);
    Field create(Field field);
    Field update(Long id, Field field);
    void delete(Long id);
}
