package com.dataspec.field.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dataspec.common.result.PageResult;
import com.dataspec.common.result.R;
import com.dataspec.field.entity.Field;
import com.dataspec.field.model.FieldBulkUpdatePreview;
import com.dataspec.field.model.FieldBulkUpdateReq;
import com.dataspec.field.model.FieldBulkUpdateResult;
import com.dataspec.field.model.FieldChangeUndoResult;
import com.dataspec.field.model.FieldGroupSummary;
import com.dataspec.field.model.FieldGroupingBatchUpdateReq;
import com.dataspec.field.model.FieldGroupingBatchUpdateResult;
import com.dataspec.field.model.FieldSearchReq;
import com.dataspec.field.model.FieldSearchResult;
import com.dataspec.field.model.FieldSuggestion;
import com.dataspec.field.service.FieldService;
import com.dataspec.reverseimport.model.FieldSourceDetail;
import com.dataspec.reverseimport.service.ReverseImportSourceService;
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
    private final ReverseImportSourceService reverseImportSourceService;

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

    /** 查询项目字段分组摘要 */
    @GetMapping("/groups")
    public R<FieldGroupSummary> groupSummary(@RequestParam Long projectId) {
        return R.ok(fieldService.groupSummary(projectId));
    }

    /** 批量更新字段归组元数据 */
    @PostMapping("/groups/batch-update")
    public R<FieldGroupingBatchUpdateResult> batchUpdateGrouping(
            @Valid @RequestBody FieldGroupingBatchUpdateReq req) {
        return R.ok(fieldService.batchUpdateGrouping(req));
    }

    /** 预览字段批量维护变更，不写入数据 */
    @PostMapping("/bulk-update/preview")
    public R<FieldBulkUpdatePreview> previewBulkUpdate(@Valid @RequestBody FieldBulkUpdateReq req) {
        return R.ok(fieldService.previewBulkUpdate(req));
    }

    /** 提交字段批量维护变更 */
    @PostMapping("/bulk-update")
    public R<FieldBulkUpdateResult> bulkUpdateFields(@Valid @RequestBody FieldBulkUpdateReq req) {
        return R.ok(fieldService.bulkUpdateFields(req));
    }

    /** 基于字段变更日志回退字段 */
    @PostMapping("/{id}/undo")
    public R<FieldChangeUndoResult> undoFieldChange(
            @PathVariable Long id,
            @RequestParam Long logId) {
        return R.ok(fieldService.undoFieldChange(id, logId));
    }

    /** 检索字段标准，返回命中原因和 AI 下一步建议 */
    @GetMapping("/search")
    public R<FieldSearchResult> search(
            @RequestParam Long projectId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean sensitive,
            @RequestParam(required = false) Long sourceBatchId,
            @RequestParam(required = false) Integer limit) {
        return R.ok(fieldService.search(new FieldSearchReq(
                projectId, query, category, tag, status, sensitive, sourceBatchId, limit)));
    }

    /** 根据业务描述推荐标准字段 */

    @GetMapping("/suggest")
    public R<List<FieldSuggestion>> suggest(
            @RequestParam Long projectId,
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit) {
        return R.ok(fieldService.suggest(projectId, query, limit));
    }

    /** 获取字段详情 */

    @GetMapping("/{id}")
    public R<Field> getById(@PathVariable Long id) {
        return R.ok(fieldService.getById(id));
    }

    /** 查询字段来源 */

    @GetMapping("/{id}/sources")
    public R<List<FieldSourceDetail>> listSources(@PathVariable Long id) {
        fieldService.getById(id);
        return R.ok(reverseImportSourceService.listByFieldId(id));
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
