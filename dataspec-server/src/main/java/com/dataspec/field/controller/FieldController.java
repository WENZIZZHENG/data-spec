package com.dataspec.field.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dataspec.common.result.PageResult;
import com.dataspec.common.result.R;
import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fields")
@RequiredArgsConstructor
public class FieldController {

    private final FieldService fieldService;

    @GetMapping
    public R<PageResult<Field>> page(
            @RequestParam Long projectId,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size) {
        IPage<Field> page = fieldService.page(projectId, current, size);
        return R.ok(PageResult.of(page));
    }

    @GetMapping("/all")
    public R<List<Field>> listAll(@RequestParam Long projectId) {
        return R.ok(fieldService.listByProject(projectId));
    }

    @GetMapping("/{id}")
    public R<Field> getById(@PathVariable Long id) {
        return R.ok(fieldService.getById(id));
    }

    @PostMapping
    public R<Field> create(@Valid @RequestBody FieldReq req) {
        Field field = new Field();
        field.setProjectId(req.projectId());
        field.setName(req.name());
        field.setDisplayName(req.displayName());
        field.setDataType(req.dataType());
        field.setLength(req.length());
        field.setPrecisionVal(req.precisionVal());
        field.setScaleVal(req.scaleVal());
        field.setNullable(req.nullable());
        field.setDefaultValue(req.defaultValue());
        field.setComment(req.comment());
        field.setDomainId(req.domainId());
        field.setTags(req.tags());
        return R.ok(fieldService.create(field));
    }

    @PutMapping("/{id}")
    public R<Field> update(@PathVariable Long id, @Valid @RequestBody FieldReq req) {
        Field field = new Field();
        field.setName(req.name());
        field.setDisplayName(req.displayName());
        field.setDataType(req.dataType());
        field.setLength(req.length());
        field.setPrecisionVal(req.precisionVal());
        field.setScaleVal(req.scaleVal());
        field.setNullable(req.nullable());
        field.setDefaultValue(req.defaultValue());
        field.setComment(req.comment());
        field.setDomainId(req.domainId());
        field.setTags(req.tags());
        return R.ok(fieldService.update(id, field));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        fieldService.delete(id);
        return R.ok();
    }

    public record FieldReq(
            @NotNull(message = "项目ID不能为空") Long projectId,
            @NotBlank(message = "字段名不能为空") String name,
            String displayName,
            @NotBlank(message = "数据类型不能为空") String dataType,
            Integer length,
            Integer precisionVal,
            Integer scaleVal,
            Boolean nullable,
            String defaultValue,
            String comment,
            Long domainId,
            String tags
    ) {}
}
