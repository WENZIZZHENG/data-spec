package com.dataspec.project.controller;

import com.dataspec.common.result.R;
import com.dataspec.project.entity.Project;
import com.dataspec.project.service.ProjectService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public R<List<Project>> list() {
        return R.ok(projectService.list());
    }

    @GetMapping("/{id}")
    public R<Project> getById(@PathVariable Long id) {
        return R.ok(projectService.getById(id));
    }

    @PostMapping
    public R<Project> create(@Valid @RequestBody CreateProjectReq req) {
        Project project = new Project();
        project.setName(req.name());
        project.setDescription(req.description());
        project.setDbType(req.dbType());
        return R.ok(projectService.create(project));
    }

    @PutMapping("/{id}")
    public R<Project> update(@PathVariable Long id, @Valid @RequestBody CreateProjectReq req) {
        Project project = new Project();
        project.setName(req.name());
        project.setDescription(req.description());
        project.setDbType(req.dbType());
        return R.ok(projectService.update(id, project));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return R.ok();
    }

    /** 创建/更新请求体 */
    public record CreateProjectReq(
            @NotBlank(message = "项目名称不能为空") String name,
            String description,
            String dbType
    ) {}
}
