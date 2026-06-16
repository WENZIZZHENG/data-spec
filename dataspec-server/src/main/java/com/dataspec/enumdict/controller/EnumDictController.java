package com.dataspec.enumdict.controller;

import com.dataspec.common.result.R;
import com.dataspec.enumdict.entity.EnumDict;
import com.dataspec.enumdict.entity.EnumValue;
import com.dataspec.enumdict.service.EnumDictService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/enums")
@RequiredArgsConstructor
public class EnumDictController {

    private final EnumDictService enumDictService;

    // ---- 枚举字典 ----

    @GetMapping
    public R<List<EnumDict>> list(@RequestParam Long projectId) {
        return R.ok(enumDictService.listByProject(projectId));
    }

    @GetMapping("/{id}")
    public R<EnumDict> getById(@PathVariable Long id) {
        return R.ok(enumDictService.getById(id));
    }

    @PostMapping
    public R<EnumDict> create(@Valid @RequestBody EnumDictReq req) {
        EnumDict dict = new EnumDict();
        dict.setProjectId(req.projectId());
        dict.setName(req.name());
        dict.setCode(req.code());
        dict.setDescription(req.description());
        dict.setValueType(req.valueType());
        return R.ok(enumDictService.create(dict));
    }

    @PutMapping("/{id}")
    public R<EnumDict> update(@PathVariable Long id, @Valid @RequestBody EnumDictReq req) {
        EnumDict dict = new EnumDict();
        dict.setName(req.name());
        dict.setCode(req.code());
        dict.setDescription(req.description());
        dict.setValueType(req.valueType());
        return R.ok(enumDictService.update(id, dict));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        enumDictService.delete(id);
        return R.ok();
    }

    // ---- 枚举值 ----

    @GetMapping("/{enumId}/values")
    public R<List<EnumValue>> listValues(@PathVariable Long enumId) {
        return R.ok(enumDictService.listValues(enumId));
    }

    @PostMapping("/{enumId}/values")
    public R<EnumValue> createValue(@PathVariable Long enumId, @Valid @RequestBody EnumValueReq req) {
        EnumValue value = new EnumValue();
        value.setEnumId(enumId);
        value.setValue(req.value());
        value.setLabel(req.label());
        value.setSortOrder(req.sortOrder() != null ? req.sortOrder() : 0);
        return R.ok(enumDictService.createValue(value));
    }

    @PutMapping("/values/{id}")
    public R<EnumValue> updateValue(@PathVariable Long id, @Valid @RequestBody EnumValueReq req) {
        EnumValue value = new EnumValue();
        value.setValue(req.value());
        value.setLabel(req.label());
        value.setSortOrder(req.sortOrder());
        return R.ok(enumDictService.updateValue(id, value));
    }

    @DeleteMapping("/values/{id}")
    public R<Void> deleteValue(@PathVariable Long id) {
        enumDictService.deleteValue(id);
        return R.ok();
    }

    public record EnumDictReq(
            @NotNull(message = "项目ID不能为空") Long projectId,
            @NotBlank(message = "枚举名称不能为空") String name,
            @NotBlank(message = "枚举编码不能为空") String code,
            String description,
            String valueType
    ) {}

    public record EnumValueReq(
            @NotBlank(message = "枚举值不能为空") String value,
            @NotBlank(message = "显示标签不能为空") String label,
            Integer sortOrder
    ) {}
}
