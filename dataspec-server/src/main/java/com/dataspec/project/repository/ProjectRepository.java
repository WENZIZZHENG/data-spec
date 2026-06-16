package com.dataspec.project.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dataspec.project.entity.Project;
import com.dataspec.project.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProjectRepository {

    private final ProjectMapper projectMapper;

    public Optional<Project> findById(Long id) {
        return Optional.ofNullable(projectMapper.selectById(id));
    }

    public List<Project> findAll() {
        return projectMapper.selectList(
                new LambdaQueryWrapper<Project>().orderByDesc(Project::getCreatedAt));
    }

    public boolean existsByName(String name) {
        return projectMapper.exists(
                new LambdaQueryWrapper<Project>().eq(Project::getName, name));
    }

    public boolean existsByNameExcludeId(String name, Long excludeId) {
        return projectMapper.exists(
                new LambdaQueryWrapper<Project>()
                        .eq(Project::getName, name)
                        .ne(Project::getId, excludeId));
    }

    public int insert(Project project) {
        return projectMapper.insert(project);
    }

    public int update(Project project) {
        return projectMapper.updateById(project);
    }

    public int deleteById(Long id) {
        return projectMapper.deleteById(id);
    }
}
