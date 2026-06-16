package com.dataspec.project.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.project.entity.Project;
import com.dataspec.project.repository.ProjectRepository;
import com.dataspec.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;

    @Override
    public List<Project> list() {
        return projectRepository.findAll();
    }

    @Override
    public Project getById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new BizException("项目不存在: " + id));
    }

    @Override
    public Project create(Project project) {
        if (projectRepository.existsByName(project.getName())) {
            throw new BizException("项目名称已存在: " + project.getName());
        }
        project.setDbType(project.getDbType() != null ? project.getDbType() : "postgresql");
        projectRepository.insert(project);
        return project;
    }

    @Override
    public Project update(Long id, Project project) {
        Project existing = getById(id);
        if (projectRepository.existsByNameExcludeId(project.getName(), id)) {
            throw new BizException("项目名称已存在: " + project.getName());
        }
        existing.setName(project.getName());
        existing.setDescription(project.getDescription());
        existing.setDbType(project.getDbType());
        projectRepository.update(existing);
        return existing;
    }

    @Override
    public void delete(Long id) {
        getById(id); // 确保存在
        projectRepository.deleteById(id);
    }
}
