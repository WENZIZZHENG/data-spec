package com.dataspec.project.service;

import com.dataspec.project.entity.Project;

import java.util.List;

/**
 * 项目空间服务接口
 */
public interface ProjectService {
    List<Project> list();
    Project getById(Long id);
    Project create(Project project);
    Project update(Long id, Project project);
    void delete(Long id);
}
