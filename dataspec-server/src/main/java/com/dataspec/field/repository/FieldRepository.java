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

@Repository
@RequiredArgsConstructor
public class FieldRepository {

    private final FieldMapper fieldMapper;

    public Optional<Field> findById(Long id) {
        return Optional.ofNullable(fieldMapper.selectById(id));
    }

    public IPage<Field> findByProjectId(Long projectId, int current, int size) {
        return fieldMapper.selectPage(
                new Page<>(current, size),
                new LambdaQueryWrapper<Field>()
                        .eq(Field::getProjectId, projectId)
                        .orderByAsc(Field::getName));
    }

    public List<Field> findAllByProjectId(Long projectId) {
        return fieldMapper.selectList(
                new LambdaQueryWrapper<Field>()
                        .eq(Field::getProjectId, projectId)
                        .orderByAsc(Field::getName));
    }

    public List<Field> findByDomainId(Long domainId) {
        return fieldMapper.selectList(
                new LambdaQueryWrapper<Field>()
                        .eq(Field::getDomainId, domainId)
                        .orderByAsc(Field::getName));
    }

    public boolean existsByNameInProject(String name, Long projectId) {
        return fieldMapper.exists(
                new LambdaQueryWrapper<Field>()
                        .eq(Field::getName, name)
                        .eq(Field::getProjectId, projectId));
    }

    public boolean existsByNameInProjectExcludeId(String name, Long projectId, Long excludeId) {
        return fieldMapper.exists(
                new LambdaQueryWrapper<Field>()
                        .eq(Field::getName, name)
                        .eq(Field::getProjectId, projectId)
                        .ne(Field::getId, excludeId));
    }

    public int insert(Field field) {
        return fieldMapper.insert(field);
    }

    public int update(Field field) {
        return fieldMapper.updateById(field);
    }

    public int deleteById(Long id) {
        return fieldMapper.deleteById(id);
    }
}
