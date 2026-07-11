package com.dataspec.enumdict.controller;

import com.dataspec.common.result.R;
import com.dataspec.enumdict.entity.EnumDict;
import com.dataspec.enumdict.entity.EnumValue;
import com.dataspec.enumdict.service.EnumDictService;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 枚举字典管理 API
 */

@RestController
@RequestMapping("/api/enums")
@RequiredArgsConstructor
public class EnumDictController {

    private final EnumDictService enumDictService;

    // ---- 枚举字典 ----

    /** 查询枚举字典列表 */

    @GetMapping
    public R<List<EnumDict>> list(@RequestParam Long projectId) {
        return R.ok(enumDictService.listByProject(projectId));
    }

    /** 获取枚举字典详情 */

    @GetMapping("/{id}")
    public R<EnumDict> getById(@PathVariable Long id) {
        return R.ok(enumDictService.getById(id));
    }

    /** 创建枚举字典 */

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

    /** 更新枚举字典 */

    @PutMapping("/{id}")
    public R<EnumDict> update(@PathVariable Long id, @Valid @RequestBody EnumDictReq req) {
        EnumDict dict = new EnumDict();
        dict.setName(req.name());
        dict.setCode(req.code());
        dict.setDescription(req.description());
        dict.setValueType(req.valueType());
        return R.ok(enumDictService.update(id, dict));
    }

    /** 删除枚举字典 */

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        enumDictService.delete(id);
        return R.ok();
    }

    // ---- 枚举值 ----

    /** 查询枚举值列表 */

    @GetMapping("/{enumId}/values")
    public R<List<EnumValue>> listValues(@PathVariable Long enumId) {
        return R.ok(enumDictService.listValues(enumId));
    }

    /** 创建枚举值 */

    @PostMapping("/{enumId}/values")
    public R<EnumValue> createValue(@PathVariable Long enumId, @Valid @RequestBody EnumValueReq req) {
        EnumValue value = new EnumValue();
        value.setEnumId(enumId);
        value.setValue(req.value());
        value.setLabel(req.label());
        value.setSortOrder(req.sortOrder() != null ? req.sortOrder() : 0);
        value.setStatus(req.status());
        value.setAliasesJson(req.aliasesJson());
        value.setReplacementValue(req.replacementValue());
        value.setValidFrom(req.validFrom());
        value.setValidTo(req.validTo());
        value.setSourceEvidence(req.sourceEvidence());
        value.setMappingHints(req.mappingHints());
        value.setAiUsageNotes(req.aiUsageNotes());
        return R.ok(enumDictService.createValue(value));
    }

    /** 更新枚举值 */

    @PutMapping("/values/{id}")
    public R<EnumValue> updateValue(@PathVariable Long id, @Valid @RequestBody EnumValueReq req) {
        EnumValue value = new EnumValue();
        value.setValue(req.value());
        value.setLabel(req.label());
        value.setSortOrder(req.sortOrder());
        value.setStatus(req.status());
        value.setAliasesJson(req.aliasesJson());
        value.setReplacementValue(req.replacementValue());
        value.setValidFrom(req.validFrom());
        value.setValidTo(req.validTo());
        value.setSourceEvidence(req.sourceEvidence());
        value.setMappingHints(req.mappingHints());
        value.setAiUsageNotes(req.aiUsageNotes());
        return R.ok(enumDictService.updateValue(id, value));
    }

    /** 删除枚举值 */

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
            Integer sortOrder,
            @Schema(description = "枚举值生命周期状态：enabled、deprecated、disabled 或 draft。")
            String status,
            @Schema(description = "枚举值别名数组 JSON，用于 AI 识别历史值、展示值或外部系统映射。")
            String aliasesJson,
            @Schema(description = "废弃或停用枚举值的推荐替代值，仅作 guidance，不自动改写 SQL。")
            String replacementValue,
            @Schema(description = "枚举值有效期开始日期，可为空。")
            LocalDate validFrom,
            @Schema(description = "枚举值有效期结束日期，可为空。")
            LocalDate validTo,
            @Schema(description = "枚举值来源证据或维护说明；不得包含凭据或业务数据行。")
            String sourceEvidence,
            @Schema(description = "枚举值跨系统映射提示，如外部编码、展示名或兼容说明。")
            String mappingHints,
            @Schema(description = "枚举值 AI 使用说明；不得包含 token、密码、完整 JDBC URL、DSN 或业务数据行。")
            String aiUsageNotes
    ) {}
}
