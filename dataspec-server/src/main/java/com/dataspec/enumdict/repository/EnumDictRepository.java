package com.dataspec.enumdict.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dataspec.enumdict.entity.EnumDict;
import com.dataspec.enumdict.entity.EnumValue;
import com.dataspec.enumdict.mapper.EnumDictMapper;
import com.dataspec.enumdict.mapper.EnumValueMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class EnumDictRepository {

    private final EnumDictMapper enumDictMapper;
    private final EnumValueMapper enumValueMapper;

    // ---- EnumDict ----

    public Optional<EnumDict> findDictById(Long id) {
        return Optional.ofNullable(enumDictMapper.selectById(id));
    }

    public List<EnumDict> findDictsByProjectId(Long projectId) {
        return enumDictMapper.selectList(
                new LambdaQueryWrapper<EnumDict>()
                        .eq(EnumDict::getProjectId, projectId)
                        .orderByAsc(EnumDict::getCode));
    }

    public boolean existsDictByCodeInProject(String code, Long projectId) {
        return enumDictMapper.exists(
                new LambdaQueryWrapper<EnumDict>()
                        .eq(EnumDict::getCode, code)
                        .eq(EnumDict::getProjectId, projectId));
    }

    public int insertDict(EnumDict enumDict) {
        return enumDictMapper.insert(enumDict);
    }

    public int updateDict(EnumDict enumDict) {
        return enumDictMapper.updateById(enumDict);
    }

    public int deleteDictById(Long id) {
        return enumDictMapper.deleteById(id);
    }

    // ---- EnumValue ----

    public List<EnumValue> findValuesByEnumId(Long enumId) {
        return enumValueMapper.selectList(
                new LambdaQueryWrapper<EnumValue>()
                        .eq(EnumValue::getEnumId, enumId)
                        .orderByAsc(EnumValue::getSortOrder));
    }

    public int insertValue(EnumValue value) {
        return enumValueMapper.insert(value);
    }

    public int updateValue(EnumValue value) {
        return enumValueMapper.updateById(value);
    }

    public int deleteValueById(Long id) {
        return enumValueMapper.deleteById(id);
    }

    public int deleteValuesByEnumId(Long enumId) {
        return enumValueMapper.delete(
                new LambdaQueryWrapper<EnumValue>()
                        .eq(EnumValue::getEnumId, enumId));
    }
}
