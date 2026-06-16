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

/**
 * 枚举字典 Repository —— 封装枚举字典和枚举值的业务查询
 */

@Repository
@RequiredArgsConstructor
public class EnumDictRepository {

    private final EnumDictMapper enumDictMapper;
    private final EnumValueMapper enumValueMapper;

    // ---- EnumDict ----

    /** 根据 ID 查找枚举字典 */
    public Optional<EnumDict> findDictById(Long id) {
        return Optional.ofNullable(enumDictMapper.selectById(id));
    }

    /** 查询项目下所有枚举字典 */
    public List<EnumDict> findDictsByProjectId(Long projectId) {
        return enumDictMapper.selectList(
                new LambdaQueryWrapper<EnumDict>()
                        .eq(EnumDict::getProjectId, projectId)
                        .orderByAsc(EnumDict::getCode));
    }

    /** 检查项目内枚举编码是否重复 */
    public boolean existsDictByCodeInProject(String code, Long projectId) {
        return enumDictMapper.exists(
                new LambdaQueryWrapper<EnumDict>()
                        .eq(EnumDict::getCode, code)
                        .eq(EnumDict::getProjectId, projectId));
    }

    /** 新增枚举字典 */
    public int insertDict(EnumDict enumDict) {
        return enumDictMapper.insert(enumDict);
    }

    /** 更新枚举字典 */
    public int updateDict(EnumDict enumDict) {
        return enumDictMapper.updateById(enumDict);
    }

    /** 逻辑删除枚举字典 */
    public int deleteDictById(Long id) {
        return enumDictMapper.deleteById(id);
    }

    // ---- EnumValue ----

    /** 查询枚举下的所有值（按排序升序） */
    public List<EnumValue> findValuesByEnumId(Long enumId) {
        return enumValueMapper.selectList(
                new LambdaQueryWrapper<EnumValue>()
                        .eq(EnumValue::getEnumId, enumId)
                        .orderByAsc(EnumValue::getSortOrder));
    }

    /** 新增枚举值 */
    public int insertValue(EnumValue value) {
        return enumValueMapper.insert(value);
    }

    /** 更新枚举值 */
    public int updateValue(EnumValue value) {
        return enumValueMapper.updateById(value);
    }

    /** 逻辑删除枚举值 */
    public int deleteValueById(Long id) {
        return enumValueMapper.deleteById(id);
    }

    /** 删除枚举下的所有值 */
    public int deleteValuesByEnumId(Long enumId) {
        return enumValueMapper.delete(
                new LambdaQueryWrapper<EnumValue>()
                        .eq(EnumValue::getEnumId, enumId));
    }
}
