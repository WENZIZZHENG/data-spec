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
import io.swagger.v3.oas.annotations.media.Schema;
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
        field.setReplacementFieldId(req.replacementFieldId());
        field.setReplacementReason(req.replacementReason());
        field.setExampleValue(req.exampleValue());
        field.setFormatType(req.formatType());
        field.setFormatPattern(req.formatPattern());
        field.setFormatUnit(req.formatUnit());
        field.setFormatPrecision(req.formatPrecision());
        field.setFormatTimezone(req.formatTimezone());
        field.setFormatNullPolicy(req.formatNullPolicy());
        field.setValidExamplesJson(req.validExamplesJson());
        field.setInvalidExamplesJson(req.invalidExamplesJson());
        field.setFormatNotes(req.formatNotes());
        field.setPreferredUseCases(req.preferredUseCases());
        field.setAvoidWhen(req.avoidWhen());
        field.setJoinHints(req.joinHints());
        field.setDefaultFilters(req.defaultFilters());
        field.setAggregationHints(req.aggregationHints());
        field.setReplacementGuidance(req.replacementGuidance());
        field.setMisuseExamples(req.misuseExamples());
        field.setLocalizedNamesJson(req.localizedNamesJson());
        field.setPreferredEnglishName(req.preferredEnglishName());
        field.setForbiddenTranslationsJson(req.forbiddenTranslationsJson());
        field.setTranslationAliasesJson(req.translationAliasesJson());
        field.setTranslationConfidence(req.translationConfidence());
        field.setTranslationNotes(req.translationNotes());
        field.setSemanticSummary(req.semanticSummary());
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
        field.setReplacementFieldId(req.replacementFieldId());
        field.setReplacementReason(req.replacementReason());
        field.setExampleValue(req.exampleValue());
        field.setFormatType(req.formatType());
        field.setFormatPattern(req.formatPattern());
        field.setFormatUnit(req.formatUnit());
        field.setFormatPrecision(req.formatPrecision());
        field.setFormatTimezone(req.formatTimezone());
        field.setFormatNullPolicy(req.formatNullPolicy());
        field.setValidExamplesJson(req.validExamplesJson());
        field.setInvalidExamplesJson(req.invalidExamplesJson());
        field.setFormatNotes(req.formatNotes());
        field.setPreferredUseCases(req.preferredUseCases());
        field.setAvoidWhen(req.avoidWhen());
        field.setJoinHints(req.joinHints());
        field.setDefaultFilters(req.defaultFilters());
        field.setAggregationHints(req.aggregationHints());
        field.setReplacementGuidance(req.replacementGuidance());
        field.setMisuseExamples(req.misuseExamples());
        field.setLocalizedNamesJson(req.localizedNamesJson());
        field.setPreferredEnglishName(req.preferredEnglishName());
        field.setForbiddenTranslationsJson(req.forbiddenTranslationsJson());
        field.setTranslationAliasesJson(req.translationAliasesJson());
        field.setTranslationConfidence(req.translationConfidence());
        field.setTranslationNotes(req.translationNotes());
        field.setSemanticSummary(req.semanticSummary());
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
            Long replacementFieldId,
            String replacementReason,
            String exampleValue,
            String formatType,
            String formatPattern,
            String formatUnit,
            String formatPrecision,
            String formatTimezone,
            String formatNullPolicy,
            String validExamplesJson,
            String invalidExamplesJson,
            String formatNotes,
            @Schema(description = "字段推荐使用场景，说明字段适合用于哪些 SQL、指标、写入或 DDL 场景；不得包含凭据或业务数据行。")
            String preferredUseCases,
            @Schema(description = "字段禁用或需确认场景，AI 命中这些场景时不得直接采纳；不得包含密码、token、完整 JDBC URL、DSN 或私钥。")
            String avoidWhen,
            @Schema(description = "字段 Join 使用提示，如推荐关联键、关联方向或不适合 Join 的边界；只做只读指导。")
            String joinHints,
            @Schema(description = "字段默认过滤条件或统计口径提示，如状态、时间范围或软删除条件；不自动改写 SQL。")
            String defaultFilters,
            @Schema(description = "字段聚合口径提示，如 sum/count/distinct/单位换算；不替代指标平台。")
            String aggregationHints,
            @Schema(description = "字段在特定场景下的替代字段或迁移指导，与生命周期替代说明互补。")
            String replacementGuidance,
            @Schema(description = "字段常见误用或反例说明，用于 AI 低置信提示；不得包含真实业务数据行或凭据。")
            String misuseExamples,
            @Schema(description = "字段本地化名称 JSON，如中文名、英文名或业务别名；不得包含凭据或业务数据行。")
            String localizedNamesJson,
            @Schema(description = "推荐英文标准字段名或命名片段，用于 AI 命名建议和翻译辅助。")
            String preferredEnglishName,
            @Schema(description = "禁用翻译数组 JSON，AI 命中后需要提示不要直接采用。")
            String forbiddenTranslationsJson,
            @Schema(description = "翻译别名数组 JSON，用于搜索、推荐和 AI Context 命名匹配。")
            String translationAliasesJson,
            @Schema(description = "命名翻译置信度，如 high、medium、low，仅作为人工维护提示。")
            String translationConfidence,
            @Schema(description = "命名翻译说明、来源或边界；不得包含 token、JDBC URL、DSN、Authorization 或业务数据行。")
            String translationNotes,
            @Schema(description = "字段语义摘要，说明单位、口径、source of truth 或常见误用；只做 AI guidance，不执行真实计算。")
            String semanticSummary
    ) {}
}
