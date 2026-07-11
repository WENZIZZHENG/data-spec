package com.dataspec.template.controller;

import com.dataspec.common.result.R;
import com.dataspec.tablemodel.model.TableAuditPolicy;
import com.dataspec.tablemodel.model.TableForeignKeyStandard;
import com.dataspec.tablemodel.model.TableIndexStandard;
import com.dataspec.tablemodel.model.TablePrimaryKeyStandard;
import com.dataspec.tablemodel.model.TableSoftDeletePolicy;
import com.dataspec.tablemodel.model.TableStructureStandard;
import com.dataspec.tablemodel.model.TableUniqueKeyStandard;
import com.dataspec.tablemodel.service.TableStructureJsonCodec;
import com.dataspec.template.entity.Template;
import com.dataspec.template.entity.TemplateField;
import com.dataspec.template.model.TemplateResp;
import com.dataspec.template.service.TemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 表模板管理 API
 */

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;
    private final ObjectMapper objectMapper;

    // ---- 模板 ----

    /** 查询模板列表 */

    @GetMapping
    public R<List<TemplateResp>> list(@RequestParam Long projectId) {
        return R.ok(templateService.listByProject(projectId).stream()
                .map(this::toResp)
                .toList());
    }

    /** 获取模板详情 */

    @GetMapping("/{id}")
    public R<TemplateResp> getById(@PathVariable Long id) {
        return R.ok(toResp(templateService.getById(id)));
    }

    /** 创建模板 */

    @PostMapping
    public R<TemplateResp> create(@Valid @RequestBody TemplateReq req) {
        Template t = new Template();
        t.setProjectId(req.projectId());
        t.setName(req.name());
        t.setDescription(req.description());
        t.setTablePrefix(req.tablePrefix());
        applyStructure(req.structure(), t);
        return R.ok(toResp(templateService.create(t)));
    }

    /** 更新模板 */

    @PutMapping("/{id}")
    public R<TemplateResp> update(@PathVariable Long id, @Valid @RequestBody TemplateReq req) {
        Template t = new Template();
        t.setName(req.name());
        t.setDescription(req.description());
        t.setTablePrefix(req.tablePrefix());
        applyStructure(req.structure(), t);
        return R.ok(toResp(templateService.update(id, t)));
    }

    /** 删除模板 */

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        templateService.delete(id);
        return R.ok();
    }

    // ---- 模板字段 ----

    /** 查询模板字段 */

    @GetMapping("/{templateId}/fields")
    public R<List<TemplateField>> listFields(@PathVariable Long templateId) {
        return R.ok(templateService.listFields(templateId));
    }

    /** 创建模板字段 */

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

    /** 更新模板字段 */

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

    /** 删除模板字段 */

    @DeleteMapping("/fields/{id}")
    public R<Void> deleteField(@PathVariable Long id) {
        templateService.deleteField(id);
        return R.ok();
    }

    public record TemplateReq(
            @NotNull(message = "项目ID不能为空") Long projectId,
            @NotBlank(message = "模板名称不能为空") String name,
            String description,
            String tablePrefix,
            TableStructureStandard structure
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

    private TemplateResp toResp(Template template) {
        return new TemplateResp(
                template.getId(),
                template.getProjectId(),
                template.getName(),
                template.getDescription(),
                template.getTablePrefix(),
                toStructure(template),
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }

    private TableStructureStandard toStructure(Template template) {
        TableStructureJsonCodec codec = codec();
        return new TableStructureStandard(
                template.getBusinessObjectId(),
                codec.read(template.getPrimaryKeyJson(), TablePrimaryKeyStandard.class, null, "主键标准"),
                codec.readList(template.getUniqueKeysJson(), TableUniqueKeyStandard.class, "唯一键标准"),
                codec.readList(template.getIndexesJson(), TableIndexStandard.class, "索引标准"),
                codec.readList(template.getForeignKeysJson(), TableForeignKeyStandard.class, "外键标准"),
                codec.readList(template.getCheckHintsJson(), String.class, "CHECK 提示"),
                codec.read(template.getAuditPolicyJson(), TableAuditPolicy.class, null, "审计策略"),
                codec.read(template.getSoftDeletePolicyJson(), TableSoftDeletePolicy.class, null, "软删除策略"),
                codec.readList(template.getDialectNotesJson(), String.class, "方言说明"),
                template.getAiUsageNotes()
        );
    }

    private void applyStructure(TableStructureStandard structure, Template template) {
        if (structure == null) {
            return;
        }
        TableStructureJsonCodec codec = codec();
        template.setBusinessObjectId(structure.businessObjectId());
        template.setPrimaryKeyJson(codec.write(structure.primaryKey(), "主键标准"));
        template.setUniqueKeysJson(codec.write(structure.uniqueKeys(), "唯一键标准"));
        template.setIndexesJson(codec.write(structure.indexes(), "索引标准"));
        template.setForeignKeysJson(codec.write(structure.foreignKeys(), "外键标准"));
        template.setCheckHintsJson(codec.write(structure.checkHints(), "CHECK 提示"));
        template.setAuditPolicyJson(codec.write(structure.auditPolicy(), "审计策略"));
        template.setSoftDeletePolicyJson(codec.write(structure.softDeletePolicy(), "软删除策略"));
        template.setDialectNotesJson(codec.write(structure.dialectNotes(), "方言说明"));
        template.setAiUsageNotes(structure.aiUsageNotes());
    }

    private TableStructureJsonCodec codec() {
        return new TableStructureJsonCodec(objectMapper);
    }
}
