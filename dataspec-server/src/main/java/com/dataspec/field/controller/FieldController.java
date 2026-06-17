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

/**
 * 标准字段库 API
 */

@RestController
@RequestMapping("/api/fields")
@RequiredArgsConstructor
public class FieldController {

    private final FieldService fieldService;

    /** 分页查询字段 */

    @GetMapping
    public R<PageResult<Field>> page(
            @RequestParam Long projectId,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size) {
        IPage<Field> page = fieldService.page(projectId, current, size);
        return R.ok(PageResult.of(page));
    }

    /** 查询全部字段（不分页） */

    @GetMapping("/all")
    public R<List<Field>> listAll(@RequestParam Long projectId) {
        return R.ok(fieldService.listByProject(projectId));
    }

    /** 获取字段详情 */

    @GetMapping("/{id}")
    public R<Field> getById(@PathVariable Long id) {
        return R.ok(fieldService.getById(id));
    }

    /** 创建字段 */

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
        field.setAliases(req.aliases());
        field.setCategory(req.category());
        field.setCodeSetId(req.codeSetId());
        field.setSensitive(req.sensitive());
        field.setStatus(req.status());
        field.setExampleValue(req.exampleValue());
        return R.ok(fieldService.create(field));
    }

    /** 更新字段 */

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
        field.setAliases(req.aliases());
        field.setCategory(req.category());
        field.setCodeSetId(req.codeSetId());
        field.setSensitive(req.sensitive());
        field.setStatus(req.status());
        field.setExampleValue(req.exampleValue());
        return R.ok(fieldService.update(id, field));
    }

    /** 删除字段 */

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
            String tags,
            String aliases,
            String category,
            Long codeSetId,
            Boolean sensitive,
            String status,
            String exampleValue
    ) {}
}
