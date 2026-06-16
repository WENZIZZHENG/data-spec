package com.dataspec.template.controller;

import com.dataspec.common.result.R;
import com.dataspec.template.entity.Template;
import com.dataspec.template.entity.TemplateField;
import com.dataspec.template.service.TemplateService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    // ---- 模板 ----

    @GetMapping
    public R<List<Template>> list(@RequestParam Long projectId) {
        return R.ok(templateService.listByProject(projectId));
    }

    @GetMapping("/{id}")
    public R<Template> getById(@PathVariable Long id) {
        return R.ok(templateService.getById(id));
    }

    @PostMapping
    public R<Template> create(@Valid @RequestBody TemplateReq req) {
        Template t = new Template();
        t.setProjectId(req.projectId());
        t.setName(req.name());
        t.setDescription(req.description());
        t.setTablePrefix(req.tablePrefix());
        return R.ok(templateService.create(t));
    }

    @PutMapping("/{id}")
    public R<Template> update(@PathVariable Long id, @Valid @RequestBody TemplateReq req) {
        Template t = new Template();
        t.setName(req.name());
        t.setDescription(req.description());
        t.setTablePrefix(req.tablePrefix());
        return R.ok(templateService.update(id, t));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        templateService.delete(id);
        return R.ok();
    }

    // ---- 模板字段 ----

    @GetMapping("/{templateId}/fields")
    public R<List<TemplateField>> listFields(@PathVariable Long templateId) {
        return R.ok(templateService.listFields(templateId));
    }

    @PostMapping("/{templateId}/fields")
    public R<TemplateField> createField(@PathVariable Long templateId,
                                        @Valid @RequestBody TemplateFieldReq req) {
        TemplateField f = new TemplateField();
        f.setTemplateId(templateId);
        f.setFieldId(req.fieldId());
        f.setName(req.name());
        f.setDataType(req.dataType());
        f.setNullable(req.nullable());
        f.setDefaultValue(req.defaultValue());
        f.setComment(req.comment());
        f.setSortOrder(req.sortOrder() != null ? req.sortOrder() : 0);
        f.setIsRequired(req.isRequired() != null ? req.isRequired() : false);
        return R.ok(templateService.createField(f));
    }

    @PutMapping("/fields/{id}")
    public R<TemplateField> updateField(@PathVariable Long id,
                                        @Valid @RequestBody TemplateFieldReq req) {
        TemplateField f = new TemplateField();
        f.setFieldId(req.fieldId());
        f.setName(req.name());
        f.setDataType(req.dataType());
        f.setNullable(req.nullable());
        f.setDefaultValue(req.defaultValue());
        f.setComment(req.comment());
        f.setSortOrder(req.sortOrder());
        f.setIsRequired(req.isRequired());
        return R.ok(templateService.updateField(id, f));
    }

    @DeleteMapping("/fields/{id}")
    public R<Void> deleteField(@PathVariable Long id) {
        templateService.deleteField(id);
        return R.ok();
    }

    public record TemplateReq(
            @NotNull(message = "项目ID不能为空") Long projectId,
            @NotBlank(message = "模板名称不能为空") String name,
            String description,
            String tablePrefix
    ) {}

    public record TemplateFieldReq(
            Long fieldId,
            @NotBlank(message = "字段名不能为空") String name,
            @NotBlank(message = "数据类型不能为空") String dataType,
            Boolean nullable,
            String defaultValue,
            String comment,
            Integer sortOrder,
            Boolean isRequired
    ) {}
}
