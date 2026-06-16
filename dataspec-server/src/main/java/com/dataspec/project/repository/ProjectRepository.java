package com.dataspec.project.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dataspec.project.entity.Project;
import com.dataspec.project.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 项目空间 Repository —— 封装项目维度的业务查询
 */

@Repository
@RequiredArgsConstructor
public class ProjectRepository {

    private final ProjectMapper projectMapper;

    /** 根据 ID 查找项目 */
    public Optional<Project> findById(Long id) {
        return Optional.ofNullable(projectMapper.selectById(id));
    }

    /** 查询所有项目（按创建时间倒序） */
    public List<Project> findAll() {
        return projectMapper.selectList(
                new LambdaQueryWrapper<Project>().orderByDesc(Project::getCreatedAt));
    }

    /** 检查项目名称是否已存在 */
    public boolean existsByName(String name) {
        return projectMapper.exists(
                new LambdaQueryWrapper<Project>().eq(Project::getName, name));
    }

    /** 检查项目名称是否已存在（排除指定 ID） */
    public boolean existsByNameExcludeId(String name, Long excludeId) {
        return projectMapper.exists(
                new LambdaQueryWrapper<Project>()
                        .eq(Project::getName, name)
                        .ne(Project::getId, excludeId));
    }

    /** 新增项目 */
    public int insert(Project project) {
        return projectMapper.insert(project);
    }

    /** 更新项目 */
    public int update(Project project) {
        return projectMapper.updateById(project);
    }

    /** 逻辑删除项目 */
    public int deleteById(Long id) {
        return projectMapper.deleteById(id);
    }
}
