package com.dataspec.field.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dataspec.businessglossary.model.GlossaryMatch;
import com.dataspec.businessglossary.service.BusinessGlossaryService;
import com.dataspec.changelog.entity.StandardChangeLog;
import com.dataspec.changelog.service.StandardChangeLogService;
import com.dataspec.common.exception.BizException;
import com.dataspec.common.perf.PerformanceProbe;
import com.dataspec.explaintrace.model.ExplainTrace;
import com.dataspec.field.entity.Field;
import com.dataspec.field.model.FieldBulkUpdateChange;
import com.dataspec.field.model.FieldBulkUpdateItem;
import com.dataspec.field.model.FieldBulkUpdatePreview;
import com.dataspec.field.model.FieldBulkUpdateReq;
import com.dataspec.field.model.FieldBulkUpdateResult;
import com.dataspec.field.model.FieldChangeUndoResult;
import com.dataspec.field.model.FieldGroupSummary;
import com.dataspec.field.model.FieldGroupingBatchUpdateReq;
import com.dataspec.field.model.FieldGroupingBatchUpdateResult;
import com.dataspec.field.model.FieldGroupingSummaries;
import com.dataspec.field.model.FieldSearchItem;
import com.dataspec.field.model.FieldSearchReq;
import com.dataspec.field.model.FieldSearchResult;
import com.dataspec.field.model.FieldSearchSummary;
import com.dataspec.field.model.FieldSuggestion;
import com.dataspec.field.repository.FieldRepository;
import com.dataspec.field.service.FieldService;
import com.dataspec.reverseimport.repository.FieldSourceRepository;
import com.dataspec.security.context.ProjectAccessGuard;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 标准字段服务实现
 */

@Service
@RequiredArgsConstructor
public class FieldServiceImpl implements FieldService {

    private static final String STATUS_DRAFT = "draft";
    private static final String STATUS_ENABLED = "enabled";
    private static final String STATUS_DEPRECATED = "deprecated";
    private static final String STATUS_DISABLED = "disabled";
    private static final String DEFAULT_STATUS = STATUS_ENABLED;
    private static final Set<String> ALLOWED_STATUSES = Set.of(STATUS_DRAFT, STATUS_ENABLED, STATUS_DEPRECATED, STATUS_DISABLED);
    private static final int DEFAULT_SUGGEST_LIMIT = 5;
    private static final int MAX_SUGGEST_LIMIT = 20;
    private static final int DEFAULT_SEARCH_LIMIT = 20;
    private static final int MAX_SEARCH_LIMIT = 50;
    private static final long FIELD_READ_WARN_MS = 500;
    private static final long FIELD_SUGGEST_WARN_MS = 500;
    private static final long FIELD_SEARCH_WARN_MS = 500;
    private static final Set<String> GROUPING_UPDATE_KEYS = Set.of("domainId", "category", "tags");
    private static final Set<String> BULK_UPDATE_KEYS = Set.of("status", "category", "tags", "sensitive", "codeSetId", "aliases");
    private static final Map<String, String> FALLBACK_TERMS = fallbackTerms();
    private static final Map<String, SemanticGroup> SEMANTIC_GROUPS = semanticGroups();
    private static final int SPECIFIC_SEMANTIC_SCORE = 88;
    private static final int GENERIC_SEMANTIC_SCORE = 24;
    private static final String FIELD_RECOMMENDATION_DOCS = "README.md#字段推荐与字段标准检索";

    private final FieldRepository fieldRepository;
    private final FieldSourceRepository fieldSourceRepository;
    private final StandardChangeLogService changeLogService;
    private final ObjectMapper objectMapper;
    private final BusinessGlossaryService businessGlossaryService;

    @Override
    public IPage<Field> page(Long projectId, int current, int size) {
        return PerformanceProbe.measure("field.page", FIELD_READ_WARN_MS,
                "字段分页变慢时优先检查字段数量、分页大小和 ds_field(project_id) 索引",
                () -> {
                    ProjectAccessGuard.requireProjectAccess(projectId);
                    return fieldRepository.findByProjectId(projectId, current, size);
                });
    }

    @Override
    public List<Field> listByProject(Long projectId) {
        return PerformanceProbe.measure("field.listByProject", FIELD_READ_WARN_MS,
                "字段全量读取变慢时优先改用分页或按需 AI Context 裁剪",
                () -> {
                    ProjectAccessGuard.requireProjectAccess(projectId);
                    return fieldRepository.findAllByProjectId(projectId);
                });
    }

    @Override
    public FieldSearchResult search(FieldSearchReq req) {
        return PerformanceProbe.measure("field.search", FIELD_SEARCH_WARN_MS,
                "字段检索变慢时优先收窄 query/category/tag/status 或后续接入元数据缓存",
                () -> searchMeasured(req));
    }

    private FieldSearchResult searchMeasured(FieldSearchReq req) {
        if (req == null || req.projectId() == null) {
            throw new BizException("项目ID不能为空");
        }
        ProjectAccessGuard.requireProjectAccess(req.projectId());
        SearchCriteria criteria = searchCriteria(req);
        if (!criteria.hasQuery() && !criteria.hasAnyFilter()) {
            throw new BizException("字段检索需要 query 或至少一个过滤条件");
        }

        Set<Long> sourceFieldIds = loadSourceFieldIds(req.projectId(), criteria.sourceBatchId());
        List<GlossaryMatch> glossaryMatches = criteria.hasQuery()
                ? businessGlossaryService.match(req.projectId(), criteria.query())
                : List.of();
        List<Field> candidates = fieldRepository.findAllByProjectId(req.projectId()).stream()
                .filter(field -> matchesSearchFilters(field, criteria, sourceFieldIds))
                .toList();

        List<FieldSearchItem> matched = new ArrayList<>();
        for (Field field : candidates) {
            FieldSearchItem item = searchItemFor(field, criteria, glossaryMatches);
            if (item != null) {
                matched.add(item);
            }
        }
        matched.sort(Comparator
                .comparingInt(FieldSearchItem::score).reversed()
                .thenComparing(item -> nullToEmpty(item.field().getName())));

        boolean truncated = matched.size() > criteria.limit();
        List<FieldSearchItem> returned = matched.stream().limit(criteria.limit()).toList();
        FieldSearchSummary summary = new FieldSearchSummary(
                candidates.size(),
                matched.size(),
                returned.size(),
                truncated,
                criteria.appliedFilters(),
                searchHints(criteria, candidates.size(), matched.size(), truncated));

        return new FieldSearchResult(
                req.projectId(),
                criteria.query(),
                summary,
                returned,
                resultNextActions(returned, truncated));
    }

    @Override
    public Field getById(Long id) {
        Field field = fieldRepository.findById(id)
                .orElseThrow(() -> new BizException("字段不存在: " + id));
        ProjectAccessGuard.requireProjectAccess(field.getProjectId());
        return field;
    }

    @Override
    public Field create(Field field) {
        ProjectAccessGuard.requireProjectAccess(field.getProjectId());
        if (fieldRepository.existsByNameInProject(field.getName(), field.getProjectId())) {
            throw new BizException("项目内字段名已存在: " + field.getName());
        }
        field.setNullable(field.getNullable() != null ? field.getNullable() : true);
        applyPersonalMetadataDefaults(field);
        validateLifecycleReplacement(field, null);
        fieldRepository.insert(field);
        changeLogService.recordChange(
                field.getProjectId(),
                StandardChangeLogService.TARGET_FIELD,
                field.getId(),
                StandardChangeLogService.ACTION_CREATE,
                null,
                changeLogService.snapshot(field));
        return field;
    }

    @Override
    public Field update(Long id, Field field) {
        Field existing = getById(id);
        if (fieldRepository.existsByNameInProjectExcludeId(field.getName(), existing.getProjectId(), id)) {
            throw new BizException("项目内字段名已存在: " + field.getName());
        }
        String beforeJson = changeLogService.snapshot(existing);
        existing.setName(field.getName());
        existing.setDisplayName(field.getDisplayName());
        existing.setDataType(field.getDataType());
        existing.setLength(field.getLength());
        existing.setPrecisionVal(field.getPrecisionVal());
        existing.setScaleVal(field.getScaleVal());
        existing.setNullable(field.getNullable());
        existing.setDefaultValue(field.getDefaultValue());
        existing.setComment(field.getComment());
        existing.setDomainId(field.getDomainId());
        existing.setTags(field.getTags());
        existing.setAliases(field.getAliases());
        existing.setCategory(field.getCategory());
        existing.setCodeSetId(field.getCodeSetId());
        existing.setSensitive(field.getSensitive() != null ? field.getSensitive() : false);
        existing.setStatus(normalizeStatus(field.getStatus()));
        existing.setReplacementFieldId(field.getReplacementFieldId());
        existing.setReplacementReason(FieldGroupingSummaries.normalizeText(field.getReplacementReason()));
        existing.setExampleValue(field.getExampleValue());
        validateLifecycleReplacement(existing, id);
        fieldRepository.update(existing);
        changeLogService.recordChange(
                existing.getProjectId(),
                StandardChangeLogService.TARGET_FIELD,
                existing.getId(),
                StandardChangeLogService.ACTION_UPDATE,
                beforeJson,
                changeLogService.snapshot(existing));
        return existing;
    }

    @Override
    public void delete(Long id) {
        Field existing = getById(id);
        String beforeJson = changeLogService.snapshot(existing);
        fieldRepository.deleteById(id);
        changeLogService.recordChange(
                existing.getProjectId(),
                StandardChangeLogService.TARGET_FIELD,
                existing.getId(),
                StandardChangeLogService.ACTION_DELETE,
                beforeJson,
                null);
    }

    @Override
    public FieldGroupSummary groupSummary(Long projectId) {
        return PerformanceProbe.measure("field.groupSummary", FIELD_READ_WARN_MS,
                "字段分组摘要变慢时优先检查未分页字段量和分组标签基数",
                () -> {
                    ProjectAccessGuard.requireProjectAccess(projectId);
                    return FieldGroupingSummaries.fromFields(projectId, fieldRepository.findAllByProjectId(projectId));
                });
    }

    @Override
    public FieldGroupingBatchUpdateResult batchUpdateGrouping(FieldGroupingBatchUpdateReq req) {
        if (req.projectId() == null) {
            throw new BizException("项目ID不能为空");
        }
        ProjectAccessGuard.requireProjectAccess(req.projectId());
        if (req.fieldIds() == null || req.fieldIds().isEmpty()) {
            throw new BizException("字段ID不能为空");
        }
        if (req.updates() == null || req.updates().isEmpty()) {
            throw new BizException("归组更新内容不能为空");
        }
        validateGroupingKeys(req.updates());

        List<Long> fieldIds = new ArrayList<>(new LinkedHashSet<>(req.fieldIds()));
        List<Field> fields = new ArrayList<>();
        for (Long fieldId : fieldIds) {
            if (fieldId == null || fieldId <= 0) {
                throw new BizException("无效字段ID: " + fieldId);
            }
            Field field = fieldRepository.findById(fieldId)
                    .orElseThrow(() -> new BizException("字段不存在: " + fieldId));
            if (!req.projectId().equals(field.getProjectId())) {
                throw new BizException("字段不属于当前项目: " + fieldId);
            }
            fields.add(field);
        }

        int updated = 0;
        for (Field field : fields) {
            String beforeJson = changeLogService.snapshot(field);
            applyGroupingUpdates(field, req.updates());
            fieldRepository.update(field);
            changeLogService.recordChange(
                    field.getProjectId(),
                    StandardChangeLogService.TARGET_FIELD,
                    field.getId(),
                    StandardChangeLogService.ACTION_UPDATE,
                    beforeJson,
                    changeLogService.snapshot(field));
            updated += 1;
        }
        return new FieldGroupingBatchUpdateResult(req.projectId(), req.fieldIds().size(), updated);
    }

    @Override
    public FieldBulkUpdatePreview previewBulkUpdate(FieldBulkUpdateReq req) {
        return buildBulkUpdatePreview(req, loadBulkUpdateFields(req));
    }

    @Override
    @Transactional
    public FieldBulkUpdateResult bulkUpdateFields(FieldBulkUpdateReq req) {
        List<Field> fields = loadBulkUpdateFields(req);
        FieldBulkUpdatePreview preview = buildBulkUpdatePreview(req, fields);
        int updated = 0;
        for (Field field : fields) {
            List<FieldBulkUpdateChange> changes = bulkChanges(field, req.updates());
            if (changes.isEmpty()) {
                continue;
            }
            String beforeJson = changeLogService.snapshot(field);
            applyBulkUpdates(field, changes);
            fieldRepository.update(field);
            changeLogService.recordChange(
                    field.getProjectId(),
                    StandardChangeLogService.TARGET_FIELD,
                    field.getId(),
                    StandardChangeLogService.ACTION_UPDATE,
                    beforeJson,
                    changeLogService.snapshot(field));
            updated += 1;
        }
        return new FieldBulkUpdateResult(req.projectId(), preview.requestedCount(), updated, preview.unchangedCount());
    }

    @Override
    @Transactional
    public FieldChangeUndoResult undoFieldChange(Long fieldId, Long logId) {
        if (logId == null) {
            throw new BizException("变更日志ID不能为空");
        }
        Field existing = getById(fieldId);
        StandardChangeLog log = changeLogService.getById(logId);
        validateUndoLog(existing, log);
        Field before = readFieldSnapshot(log.getBeforeJson());
        validateUndoSnapshot(existing, before);
        if (fieldRepository.existsByNameInProjectExcludeId(before.getName(), existing.getProjectId(), existing.getId())) {
            throw new BizException("回退后的字段名已存在: " + before.getName());
        }
        String beforeUndoJson = changeLogService.snapshot(existing);
        restoreEditableField(existing, before);
        fieldRepository.update(existing);
        changeLogService.recordChange(
                existing.getProjectId(),
                StandardChangeLogService.TARGET_FIELD,
                existing.getId(),
                StandardChangeLogService.ACTION_UNDO,
                beforeUndoJson,
                changeLogService.snapshot(existing));
        return new FieldChangeUndoResult(existing.getProjectId(), existing.getId(), log.getId());
    }

    @Override
    public List<FieldSuggestion> suggest(Long projectId, String query, int limit) {
        return PerformanceProbe.measure("field.suggest", FIELD_SUGGEST_WARN_MS,
                "字段推荐变慢时优先使用更具体 query 或后续字段检索 API",
                () -> suggestMeasured(projectId, query, limit));
    }

    private List<FieldSuggestion> suggestMeasured(Long projectId, String query, int limit) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }
        ProjectAccessGuard.requireProjectAccess(projectId);
        if (query == null || query.isBlank()) {
            throw new BizException("字段描述不能为空");
        }

        int safeLimit = normalizeLimit(limit);
        String queryCompact = compact(query);
        Set<String> queryTokens = tokens(query);
        Set<String> querySemanticGroups = semanticGroupsForText(query);
        if (queryCompact.isBlank() && queryTokens.isEmpty()) {
            throw new BizException("字段描述缺少可匹配内容");
        }
        List<GlossaryMatch> glossaryMatches = businessGlossaryService.match(projectId, query);
        List<FieldSuggestion> suggestions = new ArrayList<>();

        for (Field field : fieldRepository.findAllByProjectId(projectId)) {
            if (!isEnabledStatus(field.getStatus())) {
                continue;
            }
            ScoredMatch match = scoreFieldWithGlossary(field, queryCompact, queryTokens, querySemanticGroups, glossaryMatches);
            if (match.score() <= 0) {
                continue;
            }
            int score = match.score();
            String reason = appendReason(match.reason(), Boolean.TRUE.equals(field.getSensitive()) ? "敏感字段" : "");
            suggestions.add(new FieldSuggestion(
                    field,
                    score,
                    reason,
                    field.getName(),
                    true,
                    List.of(fieldEvidence(field, score, reason))));
        }

        suggestions.sort(Comparator
                .comparingInt(FieldSuggestion::score).reversed()
                .thenComparing(s -> nullToEmpty(s.recommendedName())));

        if (!suggestions.isEmpty()) {
            return suggestions.stream().limit(safeLimit).toList();
        }

        return List.of(new FieldSuggestion(
                null,
                0,
                "未命中已有标准字段，按描述生成候选名",
                generateFallbackName(query),
                false,
                List.of(new ExplainTrace(
                        "FIELD_SUGGESTION",
                        null,
                        null,
                        "未命中已有标准字段，按描述生成候选名",
                        0,
                        "fallback_name",
                        FIELD_RECOMMENDATION_DOCS))));
    }

    private SearchCriteria searchCriteria(FieldSearchReq req) {
        String query = FieldGroupingSummaries.normalizeText(req.query());
        String category = FieldGroupingSummaries.normalizeText(req.category());
        String tag = FieldGroupingSummaries.normalizeText(req.tag());
        String status = normalizeOptionalStatus(req.status());
        return new SearchCriteria(
                query,
                compact(query),
                tokens(query),
                semanticGroupsForText(query),
                category,
                tag,
                status,
                req.sensitive(),
                req.sourceBatchId(),
                normalizeSearchLimit(req.limit()));
    }

    private Set<Long> loadSourceFieldIds(Long projectId, Long sourceBatchId) {
        if (sourceBatchId == null) {
            return Set.of();
        }
        if (sourceBatchId <= 0) {
            throw new BizException("无效sourceBatchId: " + sourceBatchId);
        }
        return new LinkedHashSet<>(fieldSourceRepository.findFieldIdsByProjectAndBatch(projectId, sourceBatchId));
    }

    private boolean matchesSearchFilters(Field field, SearchCriteria criteria, Set<Long> sourceFieldIds) {
        if (criteria.status() == null && !isEnabledStatus(field.getStatus())) {
            return false;
        }
        if (criteria.category() != null && !criteria.category().equals(FieldGroupingSummaries.normalizeText(field.getCategory()))) {
            return false;
        }
        if (criteria.tag() != null && !splitCsv(nullToEmpty(field.getTags())).contains(criteria.tag())) {
            return false;
        }
        if (criteria.status() != null && !criteria.status().equals(normalizeStatus(field.getStatus()))) {
            return false;
        }
        if (criteria.sensitive() != null && !criteria.sensitive().equals(Boolean.TRUE.equals(field.getSensitive()))) {
            return false;
        }
        return criteria.sourceBatchId() == null || sourceFieldIds.contains(field.getId());
    }

    private FieldSearchItem searchItemFor(Field field, SearchCriteria criteria, List<GlossaryMatch> glossaryMatches) {
        int score = 1;
        List<String> reasons = new ArrayList<>();
        if (criteria.hasQuery()) {
            ScoredMatch match = scoreFieldWithGlossary(
                    field,
                    criteria.queryCompact(),
                    criteria.queryTokens(),
                    criteria.querySemanticGroups(),
                    glossaryMatches);
            if (match.score() <= 0) {
                return null;
            }
            score = match.score();
            reasons.add(match.reason());
        } else {
            reasons.addAll(filterReasons(criteria));
        }
        if (!isEnabledStatus(field.getStatus())) {
            score = Math.max(1, score - 15);
            reasons.add("字段状态为 " + normalizeStatus(field.getStatus()));
        }
        if (Boolean.TRUE.equals(field.getSensitive())) {
            reasons.add("敏感字段");
        }
        return new FieldSearchItem(
                field,
                score,
                List.copyOf(reasons),
                recommendedUse(field),
                itemNextActions(field),
                List.of(fieldEvidence(field, score, String.join("；", reasons))));
    }

    private ExplainTrace fieldEvidence(Field field, int score, String reason) {
        return new ExplainTrace(
                "FIELD",
                field.getId(),
                null,
                reason,
                Math.max(0, Math.min(score, 100)),
                null,
                FIELD_RECOMMENDATION_DOCS);
    }

    private List<String> filterReasons(SearchCriteria criteria) {
        List<String> reasons = new ArrayList<>();
        if (criteria.category() != null) {
            reasons.add("分类过滤命中: " + criteria.category());
        }
        if (criteria.tag() != null) {
            reasons.add("标签过滤命中: " + criteria.tag());
        }
        if (criteria.status() != null) {
            reasons.add("状态过滤命中: " + criteria.status());
        }
        if (criteria.sensitive() != null) {
            reasons.add("敏感标记过滤命中: " + criteria.sensitive());
        }
        if (criteria.sourceBatchId() != null) {
            reasons.add("导入批次过滤命中: " + criteria.sourceBatchId());
        }
        return reasons.isEmpty() ? List.of("过滤条件命中") : reasons;
    }

    private String recommendedUse(Field field) {
        String status = normalizeStatus(field.getStatus());
        if (!STATUS_ENABLED.equals(status)) {
            return "谨慎使用：字段状态为 " + status + "，" + replacementGuidance(field);
        }
        if (Boolean.TRUE.equals(field.getSensitive())) {
            return "敏感字段：建表、导出或 AI Context 使用前确认脱敏和权限要求。";
        }
        String category = FieldGroupingSummaries.normalizeText(field.getCategory());
        if (category != null) {
            return "适用于 " + category + " 分类的建表、SQL 修复或字段标准补全。";
        }
        return "可作为建表、SQL 修复或字段标准补全的候选标准字段。";
    }

    private String replacementGuidance(Field field) {
        Long replacementFieldId = field.getReplacementFieldId();
        String replacementReason = FieldGroupingSummaries.normalizeText(field.getReplacementReason());
        if (replacementFieldId != null && replacementReason != null) {
            return "优先查看 replacementFieldId=" + replacementFieldId + "；" + replacementReason;
        }
        if (replacementFieldId != null) {
            return "优先查看 replacementFieldId=" + replacementFieldId + " 对应的替代字段。";
        }
        if (replacementReason != null) {
            return replacementReason;
        }
        return "尚未配置替代字段或替代说明，先确认历史兼容原因。";
    }

    private List<String> itemNextActions(Field field) {
        List<String> actions = new ArrayList<>();
        String status = normalizeStatus(field.getStatus());
        if (STATUS_ENABLED.equals(status)) {
            actions.add("优先采用标准字段名 `" + field.getName() + "`，并沿用其数据类型与注释。");
        } else {
            actions.add("该字段状态为 `" + status + "`，新建表默认不要采用；" + replacementGuidance(field));
        }
        if (field.getReplacementFieldId() != null) {
            actions.add("优先评估 replacementFieldId=" + field.getReplacementFieldId() + " 对应字段。");
        }
        if (FieldGroupingSummaries.normalizeText(field.getReplacementReason()) != null) {
            actions.add("替代说明：" + FieldGroupingSummaries.normalizeText(field.getReplacementReason()));
        }
        if (Boolean.TRUE.equals(field.getSensitive())) {
            actions.add("如用于导出或日志，先确认脱敏规则和访问边界。");
        }
        return List.copyOf(actions);
    }

    private List<String> searchHints(SearchCriteria criteria, int totalCandidates, int matchedCount, boolean truncated) {
        List<String> hints = new ArrayList<>();
        if (criteria.sourceBatchId() != null && totalCandidates == 0) {
            hints.add("sourceBatchId 未命中当前项目字段来源。");
        }
        if (matchedCount == 0) {
            hints.add("未命中字段标准；可补充字段别名、调整 query，或进入标准候选 Inbox。");
        }
        if (truncated) {
            hints.add("结果已截断；可增加更具体 query/category/tag/status 过滤。");
        }
        return List.copyOf(hints);
    }

    private List<String> resultNextActions(List<FieldSearchItem> items, boolean truncated) {
        if (items.isEmpty()) {
            return List.of("进入标准候选 Inbox 或字段推荐流程，补充缺失标准字段。");
        }
        List<String> actions = new ArrayList<>();
        actions.add("优先查看首个高分字段，并在 DDL/SQL 修复中沿用其标准字段名。");
        if (truncated) {
            actions.add("结果较多时先收窄查询条件，再导出给 AI 使用。");
        }
        return List.copyOf(actions);
    }

    private String normalizeOptionalStatus(String status) {
        String normalized = FieldGroupingSummaries.normalizeText(status);
        return normalized == null ? null : normalizeStatus(normalized);
    }

    private int normalizeSearchLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_SEARCH_LIMIT;
        }
        return Math.min(limit, MAX_SEARCH_LIMIT);
    }

    private void applyPersonalMetadataDefaults(Field field) {
        field.setSensitive(field.getSensitive() != null ? field.getSensitive() : false);
        field.setStatus(normalizeStatus(field.getStatus()));
        field.setReplacementReason(FieldGroupingSummaries.normalizeText(field.getReplacementReason()));
    }

    private void validateLifecycleReplacement(Field field, Long currentFieldId) {
        Long replacementFieldId = field.getReplacementFieldId();
        if (replacementFieldId == null) {
            return;
        }
        if (replacementFieldId <= 0) {
            throw new BizException("无效replacementFieldId: " + replacementFieldId);
        }
        if (currentFieldId != null && Objects.equals(currentFieldId, replacementFieldId)) {
            throw new BizException("替代字段不能指向自身");
        }
        Field replacement = fieldRepository.findById(replacementFieldId)
                .orElseThrow(() -> new BizException("替代字段不存在: " + replacementFieldId));
        if (!Objects.equals(field.getProjectId(), replacement.getProjectId())) {
            throw new BizException("替代字段不属于当前项目: " + replacementFieldId);
        }
    }

    private void validateGroupingKeys(Map<String, Object> updates) {
        for (String key : updates.keySet()) {
            if (!GROUPING_UPDATE_KEYS.contains(key)) {
                throw new BizException("不支持的归组字段: " + key);
            }
        }
    }

    private void applyGroupingUpdates(Field field, Map<String, Object> updates) {
        if (updates.containsKey("domainId")) {
            field.setDomainId(parseOptionalLong(updates.get("domainId"), "domainId"));
        }
        if (updates.containsKey("category")) {
            field.setCategory(FieldGroupingSummaries.normalizeText(asString(updates.get("category"))));
        }
        if (updates.containsKey("tags")) {
            field.setTags(FieldGroupingSummaries.normalizeTags(asString(updates.get("tags"))));
        }
    }

    private List<Field> loadBulkUpdateFields(FieldBulkUpdateReq req) {
        validateBulkUpdateRequest(req);
        ProjectAccessGuard.requireProjectAccess(req.projectId());
        List<Long> fieldIds = uniqueValidFieldIds(req.fieldIds());
        List<Field> fields = new ArrayList<>();
        for (Long fieldId : fieldIds) {
            Field field = fieldRepository.findById(fieldId)
                    .orElseThrow(() -> new BizException("字段不存在: " + fieldId));
            if (!req.projectId().equals(field.getProjectId())) {
                throw new BizException("字段不属于当前项目: " + fieldId);
            }
            fields.add(field);
        }
        return fields;
    }

    private void validateBulkUpdateRequest(FieldBulkUpdateReq req) {
        if (req == null || req.projectId() == null) {
            throw new BizException("项目ID不能为空");
        }
        if (req.fieldIds() == null || req.fieldIds().isEmpty()) {
            throw new BizException("字段ID不能为空");
        }
        if (req.updates() == null || req.updates().isEmpty()) {
            throw new BizException("批量维护内容不能为空");
        }
        for (String key : req.updates().keySet()) {
            if (!BULK_UPDATE_KEYS.contains(key)) {
                throw new BizException("不支持的批量维护字段: " + key);
            }
        }
    }

    private List<Long> uniqueValidFieldIds(List<Long> fieldIds) {
        List<Long> uniqueIds = new ArrayList<>(new LinkedHashSet<>(fieldIds));
        for (Long fieldId : uniqueIds) {
            if (fieldId == null || fieldId <= 0) {
                throw new BizException("无效字段ID: " + fieldId);
            }
        }
        return uniqueIds;
    }

    private FieldBulkUpdatePreview buildBulkUpdatePreview(FieldBulkUpdateReq req, List<Field> fields) {
        List<FieldBulkUpdateItem> items = new ArrayList<>();
        int changedCount = 0;
        for (Field field : fields) {
            List<FieldBulkUpdateChange> changes = bulkChanges(field, req.updates());
            if (!changes.isEmpty()) {
                changedCount += 1;
            }
            items.add(new FieldBulkUpdateItem(field.getId(), field.getName(), !changes.isEmpty(), changes));
        }
        return new FieldBulkUpdatePreview(
                req.projectId(),
                fields.size(),
                changedCount,
                items.size() - changedCount,
                List.copyOf(items));
    }

    private List<FieldBulkUpdateChange> bulkChanges(Field field, Map<String, Object> updates) {
        List<FieldBulkUpdateChange> changes = new ArrayList<>();
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            Object beforeValue = currentBulkValue(field, entry.getKey());
            Object afterValue = normalizedBulkValue(entry.getKey(), entry.getValue());
            if (!Objects.equals(beforeValue, afterValue)) {
                changes.add(new FieldBulkUpdateChange(entry.getKey(), beforeValue, afterValue));
            }
        }
        return changes;
    }

    private Object currentBulkValue(Field field, String key) {
        return switch (key) {
            case "status" -> normalizeStatus(field.getStatus());
            case "category" -> FieldGroupingSummaries.normalizeText(field.getCategory());
            case "tags" -> FieldGroupingSummaries.normalizeTags(field.getTags());
            case "sensitive" -> Boolean.TRUE.equals(field.getSensitive());
            case "codeSetId" -> field.getCodeSetId();
            case "aliases" -> normalizeCsv(field.getAliases());
            default -> throw new BizException("不支持的批量维护字段: " + key);
        };
    }

    private Object normalizedBulkValue(String key, Object value) {
        return switch (key) {
            case "status" -> normalizeStatus(asString(value));
            case "category" -> FieldGroupingSummaries.normalizeText(asString(value));
            case "tags" -> FieldGroupingSummaries.normalizeTags(asString(value));
            case "sensitive" -> parseBoolean(value);
            case "codeSetId" -> parseOptionalLong(value, "codeSetId");
            case "aliases" -> normalizeCsv(asString(value));
            default -> throw new BizException("不支持的批量维护字段: " + key);
        };
    }

    private void applyBulkUpdates(Field field, List<FieldBulkUpdateChange> changes) {
        for (FieldBulkUpdateChange change : changes) {
            switch (change.attribute()) {
                case "status" -> field.setStatus((String) change.afterValue());
                case "category" -> field.setCategory((String) change.afterValue());
                case "tags" -> field.setTags((String) change.afterValue());
                case "sensitive" -> field.setSensitive((Boolean) change.afterValue());
                case "codeSetId" -> field.setCodeSetId((Long) change.afterValue());
                case "aliases" -> field.setAliases((String) change.afterValue());
                default -> throw new BizException("不支持的批量维护字段: " + change.attribute());
            }
        }
    }

    /**
     * 回退只信任当前字段自己的变更日志，避免拿其他项目或其他字段的 before 快照覆盖目标字段。
     */
    private void validateUndoLog(Field field, StandardChangeLog log) {
        if (!Objects.equals(field.getProjectId(), log.getProjectId())) {
            throw new BizException("变更日志不属于当前项目");
        }
        if (!StandardChangeLogService.TARGET_FIELD.equals(log.getTargetType())
                || !Objects.equals(field.getId(), log.getTargetId())) {
            throw new BizException("变更日志不属于当前字段");
        }
        if (!Set.of(StandardChangeLogService.ACTION_UPDATE, StandardChangeLogService.ACTION_UNDO).contains(log.getAction())) {
            throw new BizException("仅支持回退字段更新日志");
        }
        if (log.getBeforeJson() == null || log.getBeforeJson().isBlank()) {
            throw new BizException("变更日志缺少回退快照");
        }
    }

    private Field readFieldSnapshot(String json) {
        try {
            Field field = objectMapper.readValue(json, Field.class);
            if (field.getName() == null || field.getName().isBlank()) {
                throw new BizException("变更日志快照缺少字段名");
            }
            return field;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("字段回退快照解析失败: " + e.getMessage());
        }
    }

    private void validateUndoSnapshot(Field target, Field snapshot) {
        if (snapshot.getProjectId() != null && !Objects.equals(target.getProjectId(), snapshot.getProjectId())) {
            throw new BizException("变更日志快照不属于当前项目");
        }
        if (snapshot.getId() != null && !Objects.equals(target.getId(), snapshot.getId())) {
            throw new BizException("变更日志快照不属于当前字段");
        }
    }

    private void restoreEditableField(Field target, Field snapshot) {
        target.setName(snapshot.getName());
        target.setDisplayName(snapshot.getDisplayName());
        target.setDataType(snapshot.getDataType());
        target.setLength(snapshot.getLength());
        target.setPrecisionVal(snapshot.getPrecisionVal());
        target.setScaleVal(snapshot.getScaleVal());
        target.setNullable(snapshot.getNullable());
        target.setDefaultValue(snapshot.getDefaultValue());
        target.setComment(snapshot.getComment());
        target.setDomainId(snapshot.getDomainId());
        target.setTags(FieldGroupingSummaries.normalizeTags(snapshot.getTags()));
        target.setAliases(normalizeCsv(snapshot.getAliases()));
        target.setCategory(FieldGroupingSummaries.normalizeText(snapshot.getCategory()));
        target.setCodeSetId(snapshot.getCodeSetId());
        target.setSensitive(snapshot.getSensitive() != null ? snapshot.getSensitive() : false);
        target.setStatus(normalizeStatus(snapshot.getStatus()));
        target.setReplacementFieldId(snapshot.getReplacementFieldId());
        target.setReplacementReason(FieldGroupingSummaries.normalizeText(snapshot.getReplacementReason()));
        target.setExampleValue(snapshot.getExampleValue());
    }

    private Long parseOptionalLong(Object value, String label) {
        String text = FieldGroupingSummaries.normalizeText(asString(value));
        if (text == null) {
            return null;
        }
        try {
            long parsed = Long.parseLong(text);
            if (parsed <= 0) {
                throw new NumberFormatException("not positive");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new BizException("无效" + label + ": " + value);
        }
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Boolean parseBoolean(Object value) {
        String text = FieldGroupingSummaries.normalizeText(asString(value));
        if (text == null) {
            return false;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if ("true".equalsIgnoreCase(text) || "1".equals(text)) {
            return true;
        }
        if ("false".equalsIgnoreCase(text) || "0".equals(text)) {
            return false;
        }
        throw new BizException("无效sensitive: " + value);
    }

    private String normalizeCsv(String value) {
        String normalized = FieldGroupingSummaries.normalizeText(value);
        if (normalized == null) {
            return null;
        }
        Set<String> values = new LinkedHashSet<>();
        for (String part : normalized.split("[,，]")) {
            String item = FieldGroupingSummaries.normalizeText(part);
            if (item != null) {
                values.add(item);
            }
        }
        return values.isEmpty() ? null : String.join(",", values);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return DEFAULT_STATUS;
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new BizException("无效字段状态: " + status + "，允许值: " + ALLOWED_STATUSES);
        }
        return normalized;
    }

    private boolean isEnabledStatus(String status) {
        return STATUS_ENABLED.equals(normalizeStatus(status));
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_SUGGEST_LIMIT;
        }
        return Math.min(limit, MAX_SUGGEST_LIMIT);
    }

    private ScoredMatch scoreField(Field field, String queryCompact, Set<String> queryTokens,
                                   Set<String> querySemanticGroups) {
        ScoredMatch best = ScoredMatch.none();
        best = best.max(scoreText("字段名", field.getName(), queryCompact, queryTokens, 90, 72, 28));
        best = best.max(scoreText("显示名", field.getDisplayName(), queryCompact, queryTokens, 95, 78, 36));
        best = best.max(scoreText("注释", field.getComment(), queryCompact, queryTokens, 60, 48, 18));
        best = best.max(scoreText("分类", field.getCategory(), queryCompact, queryTokens, 55, 42, 18));
        best = best.max(scoreText("标签", field.getTags(), queryCompact, queryTokens, 50, 38, 16));
        for (String alias : splitCsv(field.getAliases())) {
            best = best.max(scoreText("别名", alias, queryCompact, queryTokens, 98, 82, 32));
        }
        best = best.max(scoreSemanticGroups(querySemanticGroups, semanticGroupsForField(field)));
        return best;
    }

    private ScoredMatch scoreFieldWithGlossary(Field field, String queryCompact, Set<String> queryTokens,
                                               Set<String> querySemanticGroups,
                                               List<GlossaryMatch> glossaryMatches) {
        return scoreField(field, queryCompact, queryTokens, querySemanticGroups)
                .max(scoreGlossary(field, glossaryMatches));
    }

    private ScoredMatch scoreGlossary(Field field, List<GlossaryMatch> glossaryMatches) {
        ScoredMatch best = ScoredMatch.none();
        for (GlossaryMatch match : glossaryMatches) {
            if (match.disabledTerm() || !glossaryAppliesToField(field, match)) {
                continue;
            }
            best = best.max(new ScoredMatch(match.score(), match.reason()));
        }
        return best;
    }

    private boolean glossaryAppliesToField(Field field, GlossaryMatch match) {
        return Objects.equals(field.getId(), match.canonicalFieldId())
                || Objects.equals(field.getName(), match.canonicalFieldName())
                || match.exampleFields().contains(field.getName());
    }

    private ScoredMatch scoreSemanticGroups(Set<String> querySemanticGroups, Set<String> fieldSemanticGroups) {
        ScoredMatch best = ScoredMatch.none();
        for (String groupKey : querySemanticGroups) {
            if (!fieldSemanticGroups.contains(groupKey)) {
                continue;
            }
            SemanticGroup group = SEMANTIC_GROUPS.get(groupKey);
            if (group == null) {
                continue;
            }
            int score = group.generic() ? GENERIC_SEMANTIC_SCORE : SPECIFIC_SEMANTIC_SCORE;
            best = best.max(new ScoredMatch(score, "语义词命中: " + group.canonical()));
        }
        return best;
    }

    private ScoredMatch scoreText(String label, String value, String queryCompact, Set<String> queryTokens,
                                  int exactScore, int containsScore, int tokenScore) {
        if (value == null || value.isBlank()) {
            return ScoredMatch.none();
        }
        String valueCompact = compact(value);
        if (valueCompact.isBlank()) {
            return ScoredMatch.none();
        }
        if (valueCompact.equals(queryCompact)) {
            return new ScoredMatch(exactScore, label + "精确匹配");
        }
        if (queryCompact.contains(valueCompact) || valueCompact.contains(queryCompact)) {
            return new ScoredMatch(containsScore, label + "匹配");
        }
        for (String token : tokens(value)) {
            if (isMeaningfulToken(token) && matchesQueryToken(queryCompact, queryTokens, token)) {
                return new ScoredMatch(tokenScore, label + "关键词匹配: " + token);
            }
        }
        return ScoredMatch.none();
    }

    private static List<String> splitCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (String part : value.split(",")) {
            if (!part.isBlank()) {
                values.add(part.trim());
            }
        }
        return values;
    }

    private static Set<String> tokens(String value) {
        Set<String> tokens = new LinkedHashSet<>();
        if (value == null || value.isBlank()) {
            return tokens;
        }
        String normalized = camelToSnake(value).toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replaceAll("[^\\p{IsHan}a-z0-9]+", " ");
        for (String token : normalized.split("\\s+")) {
            if (!token.isBlank()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private static String compact(String value) {
        if (value == null) {
            return "";
        }
        return camelToSnake(value).toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{IsHan}a-z0-9]+", "");
    }

    private static String camelToSnake(String value) {
        return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2");
    }

    private static Set<String> semanticGroupsForField(Field field) {
        List<String> values = new ArrayList<>();
        values.add(field.getName());
        values.add(field.getDisplayName());
        values.add(field.getComment());
        values.add(field.getCategory());
        values.add(field.getTags());
        values.addAll(splitCsv(field.getAliases()));
        return semanticGroupsForTexts(values);
    }

    private static Set<String> semanticGroupsForTexts(Iterable<String> values) {
        Set<String> groups = new LinkedHashSet<>();
        for (String value : values) {
            groups.addAll(semanticGroupsForText(value));
        }
        return groups;
    }

    private static Set<String> semanticGroupsForText(String value) {
        Set<String> groups = new LinkedHashSet<>();
        if (value == null || value.isBlank()) {
            return groups;
        }
        String valueCompact = compact(value);
        Set<String> valueTokens = tokens(value);
        for (SemanticGroup group : SEMANTIC_GROUPS.values()) {
            for (String keyword : group.keywords()) {
                String keywordCompact = compact(keyword);
                if (keywordCompact.isBlank()) {
                    continue;
                }
                if (matchesSemanticKeyword(valueCompact, valueTokens, keywordCompact)) {
                    groups.add(group.canonical());
                    break;
                }
            }
        }
        return groups;
    }

    private static boolean matchesSemanticKeyword(String valueCompact, Set<String> valueTokens, String keywordCompact) {
        if (valueTokens.contains(keywordCompact)) {
            return true;
        }
        if (containsHan(keywordCompact)) {
            return valueCompact.contains(keywordCompact);
        }
        return keywordCompact.length() >= 3 && valueCompact.contains(keywordCompact);
    }

    private static boolean matchesQueryToken(String queryCompact, Set<String> queryTokens, String token) {
        if (queryTokens.contains(token)) {
            return true;
        }
        if (containsHan(token)) {
            return queryCompact.contains(token);
        }
        return token.length() >= 3 && queryCompact.contains(token);
    }

    private static boolean containsHan(String value) {
        return value.codePoints()
                .anyMatch(codePoint -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN);
    }

    private static boolean isMeaningfulToken(String token) {
        return token.length() >= 2;
    }

    private static String generateFallbackName(String query) {
        for (String groupKey : semanticGroupsForText(query)) {
            SemanticGroup group = SEMANTIC_GROUPS.get(groupKey);
            if (group != null && !group.generic()) {
                return group.canonical();
            }
        }
        String compactQuery = compact(query);
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, String> entry : FALLBACK_TERMS.entrySet()) {
            if (compactQuery.contains(entry.getKey()) && !parts.contains(entry.getValue())) {
                parts.add(entry.getValue());
            }
        }
        if (parts.isEmpty()) {
            parts.addAll(tokens(query).stream()
                    .filter(FieldServiceImpl::isMeaningfulToken)
                    .filter(token -> token.matches("[a-z0-9]+"))
                    .toList());
        }
        if (parts.isEmpty()) {
            return "custom_field";
        }
        if (parts.contains("mobile_no")) {
            return "mobile_no";
        }
        String joined = String.join("_", parts)
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        return joined.isBlank() ? "custom_field" : joined;
    }

    private static String appendReason(String reason, String suffix) {
        if (suffix == null || suffix.isBlank()) {
            return reason;
        }
        if (reason == null || reason.isBlank()) {
            return suffix;
        }
        return reason + "；" + suffix;
    }

    private static Map<String, String> fallbackTerms() {
        Map<String, String> terms = new LinkedHashMap<>();
        terms.put("手机号", "mobile_no");
        terms.put("手机", "mobile_no");
        terms.put("电话", "phone_no");
        terms.put("客户", "customer");
        terms.put("用户", "user");
        terms.put("订单", "order");
        terms.put("支付", "payment");
        terms.put("金额", "amount");
        terms.put("价格", "price");
        terms.put("生日", "birthday");
        terms.put("出生", "birth");
        terms.put("时间", "at");
        terms.put("日期", "date");
        terms.put("状态", "status");
        terms.put("备注", "remark");
        return terms;
    }

    private static Map<String, SemanticGroup> semanticGroups() {
        Map<String, SemanticGroup> groups = new LinkedHashMap<>();
        putGroup(groups, "user_id", false,
                "user_id", "userid", "uid", "account_id", "member_id",
                "用户编号", "用户id", "用户标识", "账号id", "会员编号");
        putGroup(groups, "mobile_no", false,
                "mobile_no", "mobileno", "mobile", "phone", "tel", "sjh",
                "手机号", "手机号码", "手机", "联系电话", "联系号码", "电话");
        putGroup(groups, "amount_cent", false,
                "amount_cent", "amountcent", "pay_amount", "payment_amount", "amount", "fee", "price", "je",
                "付款金额", "支付金额", "订单金额", "金额", "费用", "价格");
        putGroup(groups, "id_card_no", false,
                "id_card_no", "idcardno", "sfzh", "identity_no", "id_card",
                "身份证号", "身份证", "证件号码");
        putGroup(groups, "order_no", false,
                "order_no", "orderno", "order_code", "订单号", "订单编号");
        putGroup(groups, "user", true,
                "user", "customer", "member", "用户", "客户", "会员");
        putGroup(groups, "order", true,
                "order", "订单");
        putGroup(groups, "status", true,
                "status", "state", "zt", "状态");
        putGroup(groups, "time", true,
                "time", "at", "sj", "时间");
        putGroup(groups, "date", true,
                "date", "rq", "日期");
        return groups;
    }

    private static void putGroup(Map<String, SemanticGroup> groups, String canonical, boolean generic,
                                 String... keywords) {
        groups.put(canonical, new SemanticGroup(canonical, Set.of(keywords), generic));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record ScoredMatch(int score, String reason) {
        static ScoredMatch none() {
            return new ScoredMatch(0, "");
        }

        ScoredMatch max(ScoredMatch other) {
            return other.score > score ? other : this;
        }
    }

    private record SemanticGroup(String canonical, Set<String> keywords, boolean generic) {
    }

    private record SearchCriteria(
            String query,
            String queryCompact,
            Set<String> queryTokens,
            Set<String> querySemanticGroups,
            String category,
            String tag,
            String status,
            Boolean sensitive,
            Long sourceBatchId,
            int limit
    ) {
        boolean hasQuery() {
            return !queryCompact.isBlank() || !queryTokens.isEmpty();
        }

        boolean hasAnyFilter() {
            return category != null
                    || tag != null
                    || status != null
                    || sensitive != null
                    || sourceBatchId != null;
        }

        Map<String, Object> appliedFilters() {
            Map<String, Object> filters = new LinkedHashMap<>();
            if (category != null) {
                filters.put("category", category);
            }
            if (tag != null) {
                filters.put("tag", tag);
            }
            if (status != null) {
                filters.put("status", status);
            }
            if (sensitive != null) {
                filters.put("sensitive", sensitive);
            }
            if (sourceBatchId != null) {
                filters.put("sourceBatchId", sourceBatchId);
            }
            filters.put("limit", limit);
            return filters;
        }
    }
}
