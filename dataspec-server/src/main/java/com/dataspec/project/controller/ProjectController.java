package com.dataspec.project.controller;

import com.dataspec.common.result.R;
import com.dataspec.project.entity.Project;
import com.dataspec.project.service.ProjectService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 项目空间 API
 */

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    /** 查询项目列表 */

    @GetMapping
    public R<List<Project>> list() {
        return R.ok(projectService.list());
    }

    /** 获取项目详情 */

    @GetMapping("/{id}")
    public R<Project> getById(@PathVariable Long id) {
        return R.ok(projectService.getById(id));
    }

    /** 创建项目 */

    @PostMapping
    public R<Project> create(@Valid @RequestBody CreateProjectReq req) {
        Project project = new Project();
        project.setName(req.name());
        project.setDescription(req.description());
        project.setDbType(req.dbType());
        boolean importBuiltInStandards = req.importBuiltInStandards() == null || req.importBuiltInStandards();
        return R.ok(projectService.create(project, importBuiltInStandards));
    }

    /** 更新项目 */

    @PutMapping("/{id}")
    public R<Project> update(@PathVariable Long id, @Valid @RequestBody CreateProjectReq req) {
        Project project = new Project();
        project.setName(req.name());
        project.setDescription(req.description());
        project.setDbType(req.dbType());
        return R.ok(projectService.update(id, project));
    }

    /** 删除项目 */

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return R.ok();
    }

    /** 创建/更新请求体 */
    public record CreateProjectReq(
            @NotBlank(message = "项目名称不能为空") String name,
            String description,
            String dbType,
            Boolean importBuiltInStandards
    ) {}
}
