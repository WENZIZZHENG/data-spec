package com.dataspec.field.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dataspec.common.exception.BizException;
import com.dataspec.field.entity.Field;
import com.dataspec.field.repository.FieldRepository;
import com.dataspec.field.service.FieldService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FieldServiceImpl implements FieldService {

    private final FieldRepository fieldRepository;

    @Override
    public IPage<Field> page(Long projectId, int current, int size) {
        return fieldRepository.findByProjectId(projectId, current, size);
    }

    @Override
    public List<Field> listByProject(Long projectId) {
        return fieldRepository.findAllByProjectId(projectId);
    }

    @Override
    public Field getById(Long id) {
        return fieldRepository.findById(id)
                .orElseThrow(() -> new BizException("字段不存在: " + id));
    }

    @Override
    public Field create(Field field) {
        if (fieldRepository.existsByNameInProject(field.getName(), field.getProjectId())) {
            throw new BizException("项目内字段名已存在: " + field.getName());
        }
        field.setNullable(field.getNullable() != null ? field.getNullable() : true);
        fieldRepository.insert(field);
        return field;
    }

    @Override
    public Field update(Long id, Field field) {
        Field existing = getById(id);
        if (fieldRepository.existsByNameInProjectExcludeId(field.getName(), existing.getProjectId(), id)) {
            throw new BizException("项目内字段名已存在: " + field.getName());
        }
        existing.setName(field.getName());
        existing.setDisplayName(field.getDisplayName());
        existing.setDataType(field.getDataType());
        existing.setLength(field.getLength());
        existing.setPrecisionVal(field.getPrecisionVal());
        existing.setScaleVal(field.getScaleVal());
        existing.setNullable(field.getNullable());
        existing.setDefaultValue(field.getDefaultValue());
        existing.setComment(field.getComment());
        existing.setDomainId(field.getDomainId());
        existing.setTags(field.getTags());
        fieldRepository.update(existing);
        return existing;
    }

    @Override
    public void delete(Long id) {
        getById(id);
        fieldRepository.deleteById(id);
    }
}
