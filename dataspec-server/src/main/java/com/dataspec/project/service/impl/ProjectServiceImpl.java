package com.dataspec.project.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.project.entity.Project;
import com.dataspec.project.repository.ProjectRepository;
import com.dataspec.project.service.ProjectService;
import com.dataspec.standards.BuiltInStandardsImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 项目空间服务实现
 */

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final BuiltInStandardsImportService standardsImportService;

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
        return create(project, true);
    }

    @Override
    @Transactional
    public Project create(Project project, boolean importBuiltInStandards) {
        if (projectRepository.existsByName(project.getName())) {
            throw new BizException("项目名称已存在: " + project.getName());
        }
        project.setDbType(project.getDbType() != null ? project.getDbType() : "postgresql");
        projectRepository.insert(project);
        if (importBuiltInStandards) {
            standardsImportService.importBuiltInStandards(project.getId());
        }
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
