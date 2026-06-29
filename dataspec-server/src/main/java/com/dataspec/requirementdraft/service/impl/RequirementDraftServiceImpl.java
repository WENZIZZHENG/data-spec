package com.dataspec.requirementdraft.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.field.entity.Field;
import com.dataspec.field.model.FieldSearchItem;
import com.dataspec.field.model.FieldSearchReq;
import com.dataspec.field.model.FieldSearchResult;
import com.dataspec.field.model.FieldSuggestion;
import com.dataspec.field.service.FieldService;
import com.dataspec.requirementdraft.model.RequirementAmbiguousCandidate;
import com.dataspec.requirementdraft.model.RequirementAmbiguousTerm;
import com.dataspec.requirementdraft.model.RequirementDraftReq;
import com.dataspec.requirementdraft.model.RequirementDraftResult;
import com.dataspec.requirementdraft.model.RequirementMatchedField;
import com.dataspec.requirementdraft.model.RequirementMissingCandidate;
import com.dataspec.requirementdraft.model.RequirementRecommendedTemplate;
import com.dataspec.requirementdraft.service.RequirementDraftService;
import com.dataspec.standardcandidate.model.StandardCandidateCreateReq;
import com.dataspec.template.entity.Template;
import com.dataspec.template.entity.TemplateField;
import com.dataspec.template.service.TemplateService;
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

@Service
@RequiredArgsConstructor
public class RequirementDraftServiceImpl implements RequirementDraftService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 20;
    private static final Set<String> GENERIC_TERMS = Set.of("金额", "状态", "编号", "名称", "时间", "日期", "ID", "id");
    private static final List<MissingPattern> MISSING_PATTERNS = List.of(
            new MissingPattern("第三方流水号", "third_party_trade_no", "第三方流水号", "varchar(128)", 82),
            new MissingPattern("支付流水号", "payment_trade_no", "支付流水号", "varchar(128)", 82),
            new MissingPattern("流水号", "trade_no", "流水号", "varchar(128)", 70),
            new MissingPattern("支付状态", "pay_status", "支付状态", "varchar(32)", 78),
            new MissingPattern("交易状态", "trade_status", "交易状态", "varchar(32)", 76),
            new MissingPattern("订单状态", "order_status", "订单状态", "varchar(32)", 74),
            new MissingPattern("支付时间", "paid_at", "支付时间", "timestamp", 72),
            new MissingPattern("完成时间", "completed_at", "完成时间", "timestamp", 68)
    );

    private final FieldService fieldService;
    private final TemplateService templateService;

    @Override
    public RequirementDraftResult draft(RequirementDraftReq req) {
        RequestContext context = validate(req);
        String query = context.query();
        List<FieldSuggestion> suggestions = fieldService.suggest(context.projectId(), query, context.limit());
        FieldSearchResult searchResult = fieldService.search(new FieldSearchReq(
                context.projectId(), query, null, null, "enabled", null, null, context.limit()));

        List<RequirementMatchedField> matchedFields = matchedFields(suggestions);
        List<RequirementAmbiguousTerm> ambiguousTerms = ambiguousTerms(query, suggestions, searchResult);
        List<RequirementMissingCandidate> missingCandidates = missingCandidates(context, matchedFields);
        RequirementRecommendedTemplate recommendedTemplate = recommendedTemplate(context, matchedFields, missingCandidates);
        List<String> nextActions = nextActions(missingCandidates, ambiguousTerms, recommendedTemplate);
        String prompt = copyablePrompt(context, matchedFields, missingCandidates, ambiguousTerms, recommendedTemplate);

        return new RequirementDraftResult(
                context.projectId(),
                context.description(),
                context.targetTableName(),
                context.groupHint(),
                matchedFields,
                missingCandidates,
                ambiguousTerms,
                recommendedTemplate,
                nextActions,
                prompt);
    }

    private RequestContext validate(RequirementDraftReq req) {
        if (req == null || req.projectId() == null) {
            throw new BizException("项目ID不能为空");
        }
        String description = required(req.description(), "需求描述不能为空");
        String tableName = required(req.targetTableName(), "目标表名不能为空");
        int limit = req.limit() == null ? DEFAULT_LIMIT : Math.max(3, Math.min(req.limit(), MAX_LIMIT));
        return new RequestContext(
                req.projectId(),
                description,
                tableName,
                blankToNull(req.groupHint()),
                limit);
    }

    private List<RequirementMatchedField> matchedFields(List<FieldSuggestion> suggestions) {
        Map<String, RequirementMatchedField> result = new LinkedHashMap<>();
        for (FieldSuggestion suggestion : suggestions) {
            Field field = suggestion.field();
            if (field == null || !suggestion.existing()) {
                continue;
            }
            String key = field.getId() != null ? String.valueOf(field.getId()) : field.getName();
            result.putIfAbsent(key, new RequirementMatchedField(
                    field,
                    suggestion.score(),
                    List.of(suggestion.matchReason()),
                    suggestion.score() >= 70));
        }
        return result.values().stream()
                .sorted(Comparator.comparingInt(RequirementMatchedField::score).reversed()
                        .thenComparing(item -> safe(item.field().getName())))
                .toList();
    }

    private List<RequirementAmbiguousTerm> ambiguousTerms(String query, List<FieldSuggestion> suggestions,
                                                         FieldSearchResult searchResult) {
        List<RequirementAmbiguousTerm> terms = new ArrayList<>();
        for (String term : GENERIC_TERMS) {
            if (!containsIgnoreCase(query, term)) {
                continue;
            }
            Map<String, RequirementAmbiguousCandidate> candidates = new LinkedHashMap<>();
            for (FieldSuggestion suggestion : suggestions) {
                Field field = suggestion.field();
                if (field != null && fieldLooksRelated(field, term)) {
                    String key = field.getId() != null ? String.valueOf(field.getId()) : field.getName();
                    candidates.putIfAbsent(key, new RequirementAmbiguousCandidate(
                            field,
                            suggestion.score(),
                            List.of(suggestion.matchReason())));
                }
            }
            if (searchResult != null && searchResult.items() != null) {
                for (FieldSearchItem item : searchResult.items()) {
                    Field field = item.field();
                    if (field != null && fieldLooksRelated(field, term)) {
                        String key = field.getId() != null ? String.valueOf(field.getId()) : field.getName();
                        candidates.putIfAbsent(key, new RequirementAmbiguousCandidate(
                                field,
                                item.score(),
                                item.matchReasons()));
                    }
                }
            }
            if (candidates.size() >= 2) {
                terms.add(new RequirementAmbiguousTerm(
                        term,
                        "该词项可对应多个已有标准字段，请确认具体业务语义后再进入 DDL 或候选采纳。",
                        candidates.values().stream()
                                .sorted(Comparator.comparingInt(RequirementAmbiguousCandidate::score).reversed())
                                .toList()));
            }
        }
        return terms;
    }

    private List<RequirementMissingCandidate> missingCandidates(RequestContext context,
                                                               List<RequirementMatchedField> matchedFields) {
        Set<String> existingNames = new LinkedHashSet<>();
        StringBuilder matchedText = new StringBuilder();
        for (RequirementMatchedField matchedField : matchedFields) {
            Field field = matchedField.field();
            existingNames.add(safe(field.getName()).toLowerCase(Locale.ROOT));
            matchedText.append(' ')
                    .append(safe(field.getName()))
                    .append(' ')
                    .append(safe(field.getDisplayName()))
                    .append(' ')
                    .append(safe(field.getComment()))
                    .append(' ')
                    .append(safe(field.getAliases()));
        }

        List<RequirementMissingCandidate> result = new ArrayList<>();
        for (MissingPattern pattern : MISSING_PATTERNS) {
            if (!context.description().contains(pattern.term())) {
                continue;
            }
            String candidateKey = pattern.candidateName().toLowerCase(Locale.ROOT);
            if (existingNames.contains(candidateKey) || containsIgnoreCase(matchedText.toString(), pattern.term())) {
                continue;
            }
            String evidence = "需求描述包含「" + pattern.term() + "」，但字段推荐未稳定命中同名标准字段。";
            StandardCandidateCreateReq payload = new StandardCandidateCreateReq(
                    context.projectId(),
                    pattern.candidateName(),
                    pattern.displayName(),
                    pattern.dataType(),
                    pattern.displayName(),
                    "REQUIREMENT_DRAFT",
                    context.targetTableName(),
                    evidence,
                    pattern.confidence());
            result.add(new RequirementMissingCandidate(
                    pattern.candidateName(),
                    pattern.displayName(),
                    pattern.dataType(),
                    pattern.displayName(),
                    evidence,
                    pattern.confidence(),
                    payload));
        }
        return result;
    }

    private RequirementRecommendedTemplate recommendedTemplate(RequestContext context,
                                                              List<RequirementMatchedField> matchedFields,
                                                              List<RequirementMissingCandidate> missingCandidates) {
        Set<String> matchedNames = new LinkedHashSet<>();
        matchedFields.forEach(item -> matchedNames.add(safe(item.field().getName()).toLowerCase(Locale.ROOT)));
        missingCandidates.forEach(item -> matchedNames.add(item.candidateName().toLowerCase(Locale.ROOT)));

        List<TemplateScore> scores = new ArrayList<>();
        for (Template template : templateService.listByProject(context.projectId())) {
            List<String> reasons = new ArrayList<>();
            int score = 0;
            if (containsAny(context.query(), template.getName(), template.getDescription())) {
                score += 35;
                reasons.add("模板名称或描述命中需求");
            }
            if (!isBlank(template.getTablePrefix()) && context.targetTableName().startsWith(template.getTablePrefix())) {
                score += 20;
                reasons.add("目标表名匹配模板前缀 " + template.getTablePrefix());
            }
            List<TemplateField> fields = templateService.listFields(template.getId());
            int overlap = 0;
            for (TemplateField field : fields) {
                if (matchedNames.contains(safe(field.getName()).toLowerCase(Locale.ROOT))) {
                    overlap += 1;
                }
            }
            if (overlap > 0) {
                score += overlap * 18;
                reasons.add("模板字段与草案字段重合 " + overlap + " 个");
            }
            if (score > 0) {
                scores.add(new TemplateScore(template, score, reasons));
            }
        }
        return scores.stream()
                .max(Comparator.comparingInt(TemplateScore::score)
                        .thenComparing(item -> safe(item.template().getName())))
                .map(item -> new RequirementRecommendedTemplate(
                        item.template().getId(),
                        item.template().getName(),
                        item.template().getDescription(),
                        item.template().getTablePrefix(),
                        item.score(),
                        item.reasons()))
                .orElse(null);
    }

    private List<String> nextActions(List<RequirementMissingCandidate> missingCandidates,
                                     List<RequirementAmbiguousTerm> ambiguousTerms,
                                     RequirementRecommendedTemplate template) {
        List<String> actions = new ArrayList<>();
        if (!ambiguousTerms.isEmpty()) {
            actions.add("先确认歧义词对应的业务语义，再决定采用哪个标准字段。");
        }
        if (!missingCandidates.isEmpty()) {
            actions.add("将缺失候选带入标准候选 Inbox，确认后再采纳为标准字段。");
        }
        if (template != null) {
            actions.add("以推荐模板为起点进入 DDL 预览，并补齐草案字段。");
        } else {
            actions.add("未命中表模板时，可先用 matchedFields 生成最小 DDL 草案。");
        }
        actions.add("复制 Prompt 给 AI 时，要求只使用 matchedFields 中的标准字段，missingCandidates 需标注待确认。");
        return actions;
    }

    private String copyablePrompt(RequestContext context,
                                  List<RequirementMatchedField> matchedFields,
                                  List<RequirementMissingCandidate> missingCandidates,
                                  List<RequirementAmbiguousTerm> ambiguousTerms,
                                  RequirementRecommendedTemplate template) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请基于 DataSpec 草案生成建表方案。\n");
        prompt.append("targetTableName: ").append(context.targetTableName()).append('\n');
        prompt.append("description: ").append(context.description()).append('\n');
        if (!isBlank(context.groupHint())) {
            prompt.append("groupHint: ").append(context.groupHint()).append('\n');
        }
        prompt.append("matchedFields:\n");
        for (RequirementMatchedField item : matchedFields) {
            prompt.append("- ")
                    .append(item.field().getName())
                    .append(" | ")
                    .append(item.field().getDataType())
                    .append(" | ")
                    .append(safe(item.field().getComment()))
                    .append('\n');
        }
        prompt.append("missingCandidates:\n");
        for (RequirementMissingCandidate item : missingCandidates) {
            prompt.append("- ")
                    .append(item.candidateName())
                    .append(" | ")
                    .append(item.dataType())
                    .append(" | ")
                    .append(item.evidence())
                    .append('\n');
        }
        prompt.append("ambiguousTerms:\n");
        for (RequirementAmbiguousTerm item : ambiguousTerms) {
            prompt.append("- ").append(item.term()).append(": ").append(item.reason()).append('\n');
        }
        if (template != null) {
            prompt.append("recommendedTemplate: ").append(template.name()).append('\n');
        }
        prompt.append("要求：不要自动采纳 missingCandidates；输出 DDL 前先列出需要人工确认的问题。\n");
        return prompt.toString();
    }

    private boolean fieldLooksRelated(Field field, String term) {
        return containsIgnoreCase(field.getName(), term)
                || containsIgnoreCase(field.getDisplayName(), term)
                || containsIgnoreCase(field.getComment(), term)
                || containsIgnoreCase(field.getAliases(), term)
                || ("金额".equals(term) && containsIgnoreCase(field.getCategory(), "money"))
                || ("状态".equals(term) && containsIgnoreCase(field.getName(), "status"));
    }

    private boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (containsIgnoreCase(haystack, needle)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsIgnoreCase(String haystack, String needle) {
        if (isBlank(haystack) || isBlank(needle)) {
            return false;
        }
        return haystack.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    private String required(String value, String message) {
        if (isBlank(value)) {
            throw new BizException(message);
        }
        return value.trim();
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record RequestContext(Long projectId, String description, String targetTableName, String groupHint, int limit) {
        private String query() {
            return String.join(" ",
                    description,
                    targetTableName,
                    groupHint == null ? "" : groupHint).trim();
        }
    }

    private record MissingPattern(String term, String candidateName, String displayName, String dataType, int confidence) {
    }

    private record TemplateScore(Template template, int score, List<String> reasons) {
    }
}
