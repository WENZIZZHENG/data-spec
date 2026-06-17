package com.dataspec.field.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dataspec.common.exception.BizException;
import com.dataspec.field.entity.Field;
import com.dataspec.field.model.FieldSuggestion;
import com.dataspec.field.repository.FieldRepository;
import com.dataspec.field.service.FieldService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 标准字段服务实现
 */

@Service
@RequiredArgsConstructor
public class FieldServiceImpl implements FieldService {

    private static final String DEFAULT_STATUS = "enabled";
    private static final Set<String> ALLOWED_STATUSES = Set.of("enabled", "disabled", "deprecated");
    private static final int DEFAULT_SUGGEST_LIMIT = 5;
    private static final int MAX_SUGGEST_LIMIT = 20;
    private static final Map<String, String> FALLBACK_TERMS = fallbackTerms();

    private final FieldRepository fieldRepository;

    @Override
    public IPage<Field> page(Long projectId, int current, int size) {
        return fieldRepository.findByProjectId(projectId, current, size);
    }

    @Override
    public List<Field> listByProject(Long projectId) {
        return fieldRepository.findAllByProjectId(projectId);
    }

    @Override
    public Field getById(Long id) {
        return fieldRepository.findById(id)
                .orElseThrow(() -> new BizException("字段不存在: " + id));
    }

    @Override
    public Field create(Field field) {
        if (fieldRepository.existsByNameInProject(field.getName(), field.getProjectId())) {
            throw new BizException("项目内字段名已存在: " + field.getName());
        }
        field.setNullable(field.getNullable() != null ? field.getNullable() : true);
        applyPersonalMetadataDefaults(field);
        fieldRepository.insert(field);
        return field;
    }

    @Override
    public Field update(Long id, Field field) {
        Field existing = getById(id);
        if (fieldRepository.existsByNameInProjectExcludeId(field.getName(), existing.getProjectId(), id)) {
            throw new BizException("项目内字段名已存在: " + field.getName());
        }
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
        existing.setExampleValue(field.getExampleValue());
        fieldRepository.update(existing);
        return existing;
    }

    @Override
    public void delete(Long id) {
        getById(id);
        fieldRepository.deleteById(id);
    }

    @Override
    public List<FieldSuggestion> suggest(Long projectId, String query, int limit) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }
        if (query == null || query.isBlank()) {
            throw new BizException("字段描述不能为空");
        }

        int safeLimit = normalizeLimit(limit);
        String queryCompact = compact(query);
        Set<String> queryTokens = tokens(query);
        if (queryCompact.isBlank() && queryTokens.isEmpty()) {
            throw new BizException("字段描述缺少可匹配内容");
        }
        List<FieldSuggestion> suggestions = new ArrayList<>();

        for (Field field : fieldRepository.findAllByProjectId(projectId)) {
            if ("disabled".equalsIgnoreCase(nullToEmpty(field.getStatus()))) {
                continue;
            }
            ScoredMatch match = scoreField(field, queryCompact, queryTokens);
            if (match.score() <= 0) {
                continue;
            }
            int score = match.score();
            if ("deprecated".equalsIgnoreCase(nullToEmpty(field.getStatus()))) {
                score = Math.max(1, score - 15);
            }
            suggestions.add(new FieldSuggestion(field, score, match.reason(), field.getName(), true));
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
                false));
    }

    private void applyPersonalMetadataDefaults(Field field) {
        field.setSensitive(field.getSensitive() != null ? field.getSensitive() : false);
        field.setStatus(normalizeStatus(field.getStatus()));
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

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_SUGGEST_LIMIT;
        }
        return Math.min(limit, MAX_SUGGEST_LIMIT);
    }

    private ScoredMatch scoreField(Field field, String queryCompact, Set<String> queryTokens) {
        ScoredMatch best = ScoredMatch.none();
        best = best.max(scoreText("字段名", field.getName(), queryCompact, queryTokens, 90, 72, 28));
        best = best.max(scoreText("显示名", field.getDisplayName(), queryCompact, queryTokens, 95, 78, 36));
        best = best.max(scoreText("注释", field.getComment(), queryCompact, queryTokens, 60, 48, 18));
        best = best.max(scoreText("分类", field.getCategory(), queryCompact, queryTokens, 55, 42, 18));
        best = best.max(scoreText("标签", field.getTags(), queryCompact, queryTokens, 50, 38, 16));
        for (String alias : splitCsv(field.getAliases())) {
            best = best.max(scoreText("别名", alias, queryCompact, queryTokens, 98, 82, 32));
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
            if (isMeaningfulToken(token) && (queryCompact.contains(token) || queryTokens.contains(token))) {
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

    private static boolean isMeaningfulToken(String token) {
        return token.length() >= 2;
    }

    private static String generateFallbackName(String query) {
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
}
