package com.dataspec.project.service;

import com.dataspec.project.entity.Project;

import java.util.List;

public interface ProjectService {
    List<Project> list();
    Project getById(Long id);
    Project create(Project project);
    Project update(Long id, Project project);
    void delete(Long id);
}
