package com.dataspec.field.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dataspec.field.entity.Field;
import com.dataspec.field.mapper.FieldMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 标准字段 Repository —— 封装字段维度的业务查询
 */

@Repository
@RequiredArgsConstructor
public class FieldRepository {

    private final FieldMapper fieldMapper;

    /** 根据 ID 查找字段 */
    public Optional<Field> findById(Long id) {
        return Optional.ofNullable(fieldMapper.selectById(id));
    }

    /** 分页查询项目下的字段 */
    public IPage<Field> findByProjectId(Long projectId, int current, int size) {
        return fieldMapper.selectPage(
                new Page<>(current, size),
                new LambdaQueryWrapper<Field>()
                        .eq(Field::getProjectId, projectId)
                        .orderByAsc(Field::getName));
    }

    /** 查询项目下所有字段（不分页） */
    public List<Field> findAllByProjectId(Long projectId) {
        return fieldMapper.selectList(
                new LambdaQueryWrapper<Field>()
                        .eq(Field::getProjectId, projectId)
                        .orderByAsc(Field::getName));
    }

    /** 根据数据域查找字段 */
    public List<Field> findByDomainId(Long domainId) {
        return fieldMapper.selectList(
                new LambdaQueryWrapper<Field>()
                        .eq(Field::getDomainId, domainId)
                        .orderByAsc(Field::getName));
    }

    /** 根据项目内字段名查找字段 */
    public Optional<Field> findByNameInProject(String name, Long projectId) {
        return Optional.ofNullable(fieldMapper.selectOne(
                new LambdaQueryWrapper<Field>()
                        .eq(Field::getName, name)
                        .eq(Field::getProjectId, projectId)
                        .last("limit 1")));
    }

    /** 检查项目内字段名是否重复 */
    public boolean existsByNameInProject(String name, Long projectId) {
        return fieldMapper.exists(
                new LambdaQueryWrapper<Field>()
                        .eq(Field::getName, name)
                        .eq(Field::getProjectId, projectId));
    }

    /** 检查项目内字段名是否重复（排除指定 ID） */
    public boolean existsByNameInProjectExcludeId(String name, Long projectId, Long excludeId) {
        return fieldMapper.exists(
                new LambdaQueryWrapper<Field>()
                        .eq(Field::getName, name)
                        .eq(Field::getProjectId, projectId)
                        .ne(Field::getId, excludeId));
    }

    /** 新增字段 */
    public int insert(Field field) {
        return fieldMapper.insert(field);
    }

    /** 更新字段 */
    public int update(Field field) {
        return fieldMapper.updateById(field);
    }

    /** 逻辑删除字段 */
    public int deleteById(Long id) {
        return fieldMapper.deleteById(id);
    }
}
