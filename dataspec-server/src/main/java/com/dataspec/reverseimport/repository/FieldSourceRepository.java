package com.dataspec.reverseimport.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dataspec.reverseimport.entity.FieldSource;
import com.dataspec.reverseimport.mapper.FieldSourceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 标准字段来源 Repository。
 */
@Repository
@RequiredArgsConstructor
public class FieldSourceRepository {

    private final FieldSourceMapper fieldSourceMapper;

    public int insert(FieldSource source) {
        return fieldSourceMapper.insert(source);
    }

    public List<FieldSource> findByFieldId(Long fieldId) {
        return fieldSourceMapper.selectList(
                new LambdaQueryWrapper<FieldSource>()
                        .eq(FieldSource::getFieldId, fieldId)
                        .orderByDesc(FieldSource::getCreatedAt)
                        .orderByDesc(FieldSource::getId));
    }
}
