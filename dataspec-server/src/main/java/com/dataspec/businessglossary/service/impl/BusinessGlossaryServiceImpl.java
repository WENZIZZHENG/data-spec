package com.dataspec.businessglossary.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dataspec.businessglossary.entity.BusinessGlossary;
import com.dataspec.businessglossary.model.BusinessGlossaryConflictEntry;
import com.dataspec.businessglossary.model.BusinessGlossaryConflictGroup;
import com.dataspec.businessglossary.model.BusinessGlossaryConflictReport;
import com.dataspec.businessglossary.model.BusinessGlossaryConflictSummary;
import com.dataspec.businessglossary.model.BusinessGlossaryContextExport;
import com.dataspec.businessglossary.model.BusinessGlossaryContextItem;
import com.dataspec.businessglossary.model.GlossaryMatch;
import com.dataspec.businessglossary.repository.BusinessGlossaryRepository;
import com.dataspec.businessglossary.service.BusinessGlossaryService;
import com.dataspec.common.exception.BizException;
import com.dataspec.field.entity.Field;
import com.dataspec.field.repository.FieldRepository;
import com.dataspec.security.context.ProjectAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BusinessGlossaryServiceImpl implements BusinessGlossaryService {

    private static final String STATUS_ENABLED = "enabled";
    private static final String STATUS_DISABLED = "disabled";
    private static final Set<String> ALLOWED_STATUSES = Set.of(STATUS_ENABLED, STATUS_DISABLED);
    private static final Set<String> ALLOWED_SCOPES = Set.of("GLOBAL", "CATEGORY", "DOMAIN", "TAG");

    private final BusinessGlossaryRepository repository;
    private final FieldRepository fieldRepository;

    @Override
    public IPage<BusinessGlossary> page(Long projectId, String keyword, String status, int current, int size) {
        requireProject(projectId);
        return repository.findPage(projectId, trimToNull(keyword), normalizeOptionalStatus(status), current, size);
    }

    @Override
    public List<BusinessGlossary> listByProject(Long projectId, String status) {
        requireProject(projectId);
        String normalizedStatus = normalizeOptionalStatus(status);
        return repository.findAllByProjectId(projectId).stream()
                .filter(entry -> normalizedStatus == null || normalizedStatus.equals(normalizeStatus(entry.getStatus())))
                .toList();
    }

    @Override
    public BusinessGlossary getById(Long id) {
        BusinessGlossary glossary = repository.findById(id)
                .orElseThrow(() -> new BizException("业务术语不存在: " + id));
        ProjectAccessGuard.requireProjectAccess(glossary.getProjectId());
        return glossary;
    }

    @Override
    public BusinessGlossary create(BusinessGlossary glossary) {
        normalizeAndValidate(glossary, glossary == null ? null : glossary.getProjectId());
        ProjectAccessGuard.requireProjectAccess(glossary.getProjectId());
        ensureCanonicalFieldReadable(glossary);
        rejectDuplicateActiveTerm(glossary);
        repository.insert(glossary);
        return glossary;
    }

    @Override
    public BusinessGlossary update(Long id, BusinessGlossary incoming) {
        BusinessGlossary existing = getById(id);
        copyEditableFields(existing, incoming);
        normalizeAndValidate(existing, existing.getProjectId());
        ensureCanonicalFieldReadable(existing);
        rejectDuplicateActiveTerm(existing);
        repository.update(existing);
        return existing;
    }

    @Override
    public void delete(Long id) {
        BusinessGlossary existing = getById(id);
        repository.deleteById(existing.getId());
    }

    @Override
    public BusinessGlossaryConflictReport conflicts(Long projectId) {
        requireProject(projectId);
        List<BusinessGlossary> active = activeEntries(projectId);
        List<BusinessGlossaryConflictGroup> conflicts = new ArrayList<>();

        Map<String, List<BusinessGlossary>> positiveBuckets = new LinkedHashMap<>();
        Map<String, String> displayTokens = new LinkedHashMap<>();
        for (BusinessGlossary entry : active) {
            for (Token token : positiveTokens(entry)) {
                positiveBuckets.computeIfAbsent(token.normalized(), ignored -> new ArrayList<>()).add(entry);
                displayTokens.putIfAbsent(token.normalized(), token.display());
            }
        }
        for (Map.Entry<String, List<BusinessGlossary>> bucket : positiveBuckets.entrySet()) {
            List<BusinessGlossary> entries = distinctEntries(bucket.getValue());
            if (entries.size() > 1) {
                conflicts.add(conflict(
                        "DUPLICATE_TOKEN",
                        "WARNING",
                        displayTokens.get(bucket.getKey()),
                        "同一个术语/同义词/词根被多个条目使用，AI 可能无法稳定选择 canonical 字段。",
                        entries,
                        "合并重复术语，或拆分适用范围并补充说明。"));
            }
        }

        for (BusinessGlossary entry : active) {
            for (Token disabled : disabledTokens(entry)) {
                List<BusinessGlossary> positiveEntries = distinctEntries(positiveBuckets.getOrDefault(disabled.normalized(), List.of()));
                if (!positiveEntries.isEmpty()) {
                    conflicts.add(conflict(
                            "DISABLED_TERM_CONFLICT",
                            "ERROR",
                            disabled.display(),
                            "禁用词同时被其他启用条目当作正向术语使用。",
                            merge(entry, positiveEntries),
                            "确认该词是禁用词还是有效同义词，并删除冲突配置。"));
                }
            }
        }

        for (BusinessGlossary entry : active) {
            if (entry.getCanonicalFieldId() == null) {
                continue;
            }
            Optional<Field> field = fieldRepository.findById(entry.getCanonicalFieldId());
            if (field.isEmpty()
                    || !Objects.equals(field.get().getProjectId(), entry.getProjectId())
                    || STATUS_DISABLED.equalsIgnoreCase(nullToEmpty(field.get().getStatus()))) {
                conflicts.add(conflict(
                        "MISSING_CANONICAL_FIELD",
                        "WARNING",
                        String.valueOf(entry.getCanonicalFieldId()),
                        "canonical 字段 " + entry.getCanonicalFieldId() + " 不存在、跨项目或已禁用。",
                        List.of(entry),
                        "重新选择当前项目启用字段，或清空 canonical 字段后只保留示例字段。"));
            }
        }

        long errors = conflicts.stream().filter(item -> "ERROR".equals(item.severity())).count();
        BusinessGlossaryConflictSummary summary = new BusinessGlossaryConflictSummary(
                conflicts.size(),
                (int) errors,
                conflicts.size() - (int) errors);
        return new BusinessGlossaryConflictReport(projectId, summary, List.copyOf(conflicts));
    }

    @Override
    public List<GlossaryMatch> match(Long projectId, String query) {
        requireProject(projectId);
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String queryCompact = compact(query);
        Set<String> queryTokens = tokens(query);
        List<GlossaryMatch> matches = new ArrayList<>();
        for (BusinessGlossary entry : activeEntries(projectId)) {
            positiveTokens(entry).stream()
                    .filter(token -> matchesToken(queryCompact, queryTokens, token.normalized()))
                    .findFirst()
                    .ifPresent(token -> matches.add(matchOf(entry, token, false)));
            if (matches.stream().noneMatch(match -> Objects.equals(match.glossaryId(), entry.getId()))) {
                disabledTokens(entry).stream()
                        .filter(token -> matchesToken(queryCompact, queryTokens, token.normalized()))
                        .findFirst()
                        .ifPresent(token -> matches.add(matchOf(entry, token, true)));
            }
        }
        return matches.stream()
                .sorted(Comparator.comparingInt(GlossaryMatch::score).reversed())
                .toList();
    }

    @Override
    public BusinessGlossaryContextExport contextExport(Long projectId, int limit) {
        requireProject(projectId);
        int safeLimit = limit <= 0 ? 200 : limit;
        List<BusinessGlossary> active = activeEntries(projectId);
        List<BusinessGlossary> returned = active.size() > safeLimit ? active.subList(0, safeLimit) : active;
        List<BusinessGlossaryContextItem> items = returned.stream()
                .map(entry -> new BusinessGlossaryContextItem(
                        entry.getTerm(),
                        splitCsv(entry.getSynonyms()),
                        splitCsv(entry.getRootTerms()),
                        splitCsv(entry.getAbbreviations()),
                        splitCsv(entry.getDisabledTerms()),
                        canonicalFieldName(entry).orElse(null),
                        entry.getScopeType(),
                        entry.getScopeValue(),
                        splitCsv(entry.getExampleFields())))
                .toList();
        return new BusinessGlossaryContextExport(items, active.size() > safeLimit, active.size(), items.size());
    }

    private void normalizeAndValidate(BusinessGlossary glossary, Long projectId) {
        if (glossary == null) {
            throw new BizException("业务术语不能为空");
        }
        glossary.setProjectId(projectId);
        if (glossary.getProjectId() == null) {
            throw new BizException("项目ID不能为空");
        }
        glossary.setTerm(trimToNull(glossary.getTerm()));
        if (glossary.getTerm() == null) {
            throw new BizException("术语不能为空");
        }
        glossary.setSynonyms(normalizeCsv(glossary.getSynonyms()));
        glossary.setRootTerms(normalizeCsv(glossary.getRootTerms()));
        glossary.setAbbreviations(normalizeCsv(glossary.getAbbreviations()));
        glossary.setDisabledTerms(normalizeCsv(glossary.getDisabledTerms()));
        glossary.setExampleFields(normalizeCsv(glossary.getExampleFields()));
        glossary.setDescription(trimToNull(glossary.getDescription()));
        glossary.setStatus(normalizeStatus(glossary.getStatus()));
        glossary.setScopeType(normalizeScope(glossary.getScopeType()));
        glossary.setScopeValue(trimToNull(glossary.getScopeValue()));
    }

    private void ensureCanonicalFieldReadable(BusinessGlossary glossary) {
        if (glossary.getCanonicalFieldId() == null) {
            return;
        }
        fieldRepository.findById(glossary.getCanonicalFieldId()).ifPresent(field -> {
            if (!Objects.equals(field.getProjectId(), glossary.getProjectId())) {
                throw new BizException("canonical 字段不属于当前项目: " + glossary.getCanonicalFieldId());
            }
        });
    }

    private void rejectDuplicateActiveTerm(BusinessGlossary glossary) {
        if (!STATUS_ENABLED.equals(glossary.getStatus())) {
            return;
        }
        String normalizedTerm = compact(glossary.getTerm());
        boolean duplicated = repository.findAllByProjectId(glossary.getProjectId()).stream()
                .filter(entry -> !Objects.equals(entry.getId(), glossary.getId()))
                .filter(entry -> STATUS_ENABLED.equals(normalizeStatus(entry.getStatus())))
                .anyMatch(entry -> compact(entry.getTerm()).equals(normalizedTerm));
        if (duplicated) {
            throw new BizException("项目内启用术语已存在: " + glossary.getTerm());
        }
    }

    private List<BusinessGlossary> activeEntries(Long projectId) {
        return repository.findAllByProjectId(projectId).stream()
                .filter(entry -> STATUS_ENABLED.equals(normalizeStatus(entry.getStatus())))
                .toList();
    }

    private GlossaryMatch matchOf(BusinessGlossary entry, Token token, boolean disabledTerm) {
        Optional<String> canonicalFieldName = canonicalFieldName(entry);
        int score = disabledTerm ? 0 : token.score();
        String target = canonicalFieldName.orElseGet(() -> String.join(",", splitCsv(entry.getExampleFields())));
        String reason = disabledTerm
                ? "术语表禁用词：" + token.display()
                : "术语表：" + token.display() + (target.isBlank() ? "" : " -> " + target);
        return new GlossaryMatch(
                entry.getId(),
                entry.getTerm(),
                token.display(),
                token.type(),
                score,
                entry.getCanonicalFieldId(),
                canonicalFieldName.orElse(null),
                Set.copyOf(splitCsv(entry.getExampleFields())),
                disabledTerm,
                reason);
    }

    private Optional<String> canonicalFieldName(BusinessGlossary entry) {
        if (entry.getCanonicalFieldId() == null) {
            return Optional.empty();
        }
        return fieldRepository.findById(entry.getCanonicalFieldId())
                .filter(field -> Objects.equals(field.getProjectId(), entry.getProjectId()))
                .filter(field -> !STATUS_DISABLED.equalsIgnoreCase(nullToEmpty(field.getStatus())))
                .map(Field::getName);
    }

    private BusinessGlossaryConflictGroup conflict(String type, String severity, String token, String message,
                                                   List<BusinessGlossary> entries, String nextAction) {
        return new BusinessGlossaryConflictGroup(
                type,
                severity,
                token,
                message,
                entries.stream().map(this::entryRef).toList(),
                nextAction);
    }

    private BusinessGlossaryConflictEntry entryRef(BusinessGlossary entry) {
        return new BusinessGlossaryConflictEntry(
                entry.getId(),
                entry.getTerm(),
                entry.getCanonicalFieldId(),
                canonicalFieldName(entry).orElse(null));
    }

    private List<BusinessGlossary> merge(BusinessGlossary first, List<BusinessGlossary> rest) {
        List<BusinessGlossary> merged = new ArrayList<>();
        merged.add(first);
        merged.addAll(rest);
        return distinctEntries(merged);
    }

    private List<BusinessGlossary> distinctEntries(List<BusinessGlossary> entries) {
        Map<Long, BusinessGlossary> byId = new LinkedHashMap<>();
        for (BusinessGlossary entry : entries) {
            byId.put(entry.getId(), entry);
        }
        return new ArrayList<>(byId.values());
    }

    private List<Token> positiveTokens(BusinessGlossary entry) {
        List<Token> tokens = new ArrayList<>();
        addToken(tokens, entry.getTerm(), "TERM", 122);
        addTokens(tokens, entry.getSynonyms(), "SYNONYM", 116);
        addTokens(tokens, entry.getRootTerms(), "ROOT", 108);
        addTokens(tokens, entry.getAbbreviations(), "ABBREVIATION", 104);
        return tokens;
    }

    private List<Token> disabledTokens(BusinessGlossary entry) {
        List<Token> tokens = new ArrayList<>();
        addTokens(tokens, entry.getDisabledTerms(), "DISABLED", 0);
        return tokens;
    }

    private void addTokens(List<Token> tokens, String csv, String type, int score) {
        for (String value : splitCsv(csv)) {
            addToken(tokens, value, type, score);
        }
    }

    private void addToken(List<Token> tokens, String value, String type, int score) {
        String display = trimToNull(value);
        if (display == null) {
            return;
        }
        String normalized = compact(display);
        if (!normalized.isBlank()) {
            tokens.add(new Token(display, normalized, type, score));
        }
    }

    private void copyEditableFields(BusinessGlossary existing, BusinessGlossary incoming) {
        if (incoming == null) {
            throw new BizException("业务术语不能为空");
        }
        existing.setTerm(incoming.getTerm());
        existing.setSynonyms(incoming.getSynonyms());
        existing.setRootTerms(incoming.getRootTerms());
        existing.setAbbreviations(incoming.getAbbreviations());
        existing.setDisabledTerms(incoming.getDisabledTerms());
        existing.setCanonicalFieldId(incoming.getCanonicalFieldId());
        existing.setScopeType(incoming.getScopeType());
        existing.setScopeValue(incoming.getScopeValue());
        existing.setExampleFields(incoming.getExampleFields());
        existing.setDescription(incoming.getDescription());
        existing.setStatus(incoming.getStatus());
    }

    private void requireProject(Long projectId) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }
        ProjectAccessGuard.requireProjectAccess(projectId);
    }

    private String normalizeOptionalStatus(String status) {
        String normalized = trimToNull(status);
        return normalized == null ? null : normalizeStatus(normalized);
    }

    private String normalizeStatus(String status) {
        String normalized = trimToNull(status);
        normalized = normalized == null ? STATUS_ENABLED : normalized.toLowerCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new BizException("不支持的术语状态: " + status);
        }
        return normalized;
    }

    private String normalizeScope(String scopeType) {
        String normalized = trimToNull(scopeType);
        normalized = normalized == null ? "GLOBAL" : normalized.toUpperCase(Locale.ROOT);
        if (!ALLOWED_SCOPES.contains(normalized)) {
            throw new BizException("不支持的术语范围: " + scopeType);
        }
        return normalized;
    }

    private String normalizeCsv(String value) {
        List<String> values = splitCsv(value);
        if (values.isEmpty()) {
            return null;
        }
        return String.join(",", values);
    }

    private List<String> splitCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        Set<String> values = new LinkedHashSet<>();
        for (String part : value.split(",")) {
            String trimmed = trimToNull(part);
            if (trimmed != null) {
                values.add(trimmed);
            }
        }
        return List.copyOf(values);
    }

    private boolean matchesToken(String queryCompact, Set<String> queryTokens, String token) {
        if (token.isBlank()) {
            return false;
        }
        if (queryTokens.contains(token)) {
            return true;
        }
        if (containsHan(token)) {
            return queryCompact.contains(token);
        }
        return token.length() >= 2 && queryCompact.contains(token);
    }

    private Set<String> tokens(String value) {
        Set<String> tokens = new LinkedHashSet<>();
        if (value == null || value.isBlank()) {
            return tokens;
        }
        String normalized = value.toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replaceAll("[^\\p{IsHan}a-z0-9]+", " ");
        for (String token : normalized.split("\\s+")) {
            if (!token.isBlank()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private String compact(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{IsHan}a-z0-9]+", "");
    }

    private boolean containsHan(String value) {
        return value.codePoints()
                .anyMatch(codePoint -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record Token(String display, String normalized, String type, int score) {
    }
}
