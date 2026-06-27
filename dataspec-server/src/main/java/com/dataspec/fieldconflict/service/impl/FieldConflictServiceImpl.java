package com.dataspec.fieldconflict.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.fieldconflict.model.FieldConflictField;
import com.dataspec.fieldconflict.model.FieldConflictGroup;
import com.dataspec.fieldconflict.model.FieldConflictReport;
import com.dataspec.fieldconflict.model.FieldConflictSeverity;
import com.dataspec.fieldconflict.model.FieldConflictSummary;
import com.dataspec.fieldconflict.model.FieldConflictType;
import com.dataspec.fieldconflict.service.FieldConflictService;
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
 * 项目内字段冲突实时检测服务。
 *
 * <p>第一版只做确定性只读扫描，不自动合并或修改字段库，避免把疑似重复误处理成破坏性操作。</p>
 */
@Service
@RequiredArgsConstructor
public class FieldConflictServiceImpl implements FieldConflictService {

    private static final Map<String, SemanticGroup> SEMANTIC_GROUPS = semanticGroups();

    private final FieldService fieldService;

    @Override
    public FieldConflictReport report(Long projectId) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }
        List<Field> fields = fieldService.listByProject(projectId);
        FieldConflictReport report = new FieldConflictReport();
        report.setProjectId(projectId);
        report.getSummary().setTotalFieldCount(fields.size());

        List<FieldConflictGroup> groups = report.getGroups();
        groups.addAll(detectNameDuplicates(fields));
        groups.addAll(detectAliasConflicts(fields));
        groups.addAll(detectDisplayNameDuplicates(fields));
        groups.addAll(detectSemanticDuplicates(fields));
        groups.sort(Comparator
                .comparing((FieldConflictGroup group) -> severityRank(group.getSeverity()))
                .thenComparing(group -> nullToEmpty(group.getConflictType() == null ? null : group.getConflictType().name()))
                .thenComparing(group -> nullToEmpty(group.getGroupKey())));

        buildSummary(report);
        return report;
    }

    private List<FieldConflictGroup> detectNameDuplicates(List<Field> fields) {
        Map<String, List<Field>> byName = groupByNormalized(fields, Field::getName);
        List<FieldConflictGroup> groups = new ArrayList<>();
        for (Map.Entry<String, List<Field>> entry : byName.entrySet()) {
            if (entry.getValue().size() <= 1) {
                continue;
            }
            groups.add(group(
                    "name:" + entry.getKey(),
                    FieldConflictType.NAME_DUPLICATE,
                    "字段名重复: " + entry.getKey(),
                    "多个标准字段使用相同字段名，会导致生成和导入时无法确定唯一标准。",
                    entry.getValue(),
                    List.of("字段名重复: " + entry.getKey()),
                    "保留一个标准字段，其余字段改名、废弃或合并别名。"
            ));
        }
        return groups;
    }

    private List<FieldConflictGroup> detectAliasConflicts(List<Field> fields) {
        Map<String, List<Field>> byAlias = new LinkedHashMap<>();
        Map<String, Field> fieldByName = new LinkedHashMap<>();
        for (Field field : fields) {
            String normalizedName = normalize(field.getName());
            if (!normalizedName.isBlank()) {
                fieldByName.put(normalizedName, field);
            }
            for (String alias : splitCsv(field.getAliases())) {
                String normalizedAlias = normalize(alias);
                if (normalizedAlias.isBlank()) {
                    continue;
                }
                byAlias.computeIfAbsent(normalizedAlias, ignored -> new ArrayList<>()).add(field);
            }
        }

        List<FieldConflictGroup> groups = new ArrayList<>();
        for (Map.Entry<String, List<Field>> entry : byAlias.entrySet()) {
            LinkedHashSet<Field> involved = new LinkedHashSet<>(entry.getValue());
            Field ownerByName = fieldByName.get(entry.getKey());
            if (ownerByName != null) {
                involved.add(ownerByName);
            }
            if (involved.size() <= 1) {
                continue;
            }
            List<Field> conflictFields = new ArrayList<>(involved);
            List<String> evidence = new ArrayList<>();
            evidence.add("别名被多个字段占用或等于字段名: " + entry.getKey());
            evidence.addAll(attributeMismatchEvidence(conflictFields));
            groups.add(group(
                    "alias:" + entry.getKey(),
                    FieldConflictType.ALIAS_CONFLICT,
                    "别名冲突: " + entry.getKey(),
                    "同一别名指向多个标准字段，AI 推荐和反向导入可能命中错误字段。",
                    conflictFields,
                    evidence,
                    "确认该别名应归属的唯一字段；其他字段移除别名、改成更具体别名或废弃。"
            ));
        }
        return groups;
    }

    private List<FieldConflictGroup> detectDisplayNameDuplicates(List<Field> fields) {
        Map<String, List<Field>> byDisplayName = groupByNormalized(fields, Field::getDisplayName);
        List<FieldConflictGroup> groups = new ArrayList<>();
        for (Map.Entry<String, List<Field>> entry : byDisplayName.entrySet()) {
            if (entry.getValue().size() <= 1) {
                continue;
            }
            List<String> evidence = new ArrayList<>();
            evidence.add("显示名重复: " + entry.getKey());
            evidence.addAll(attributeMismatchEvidence(entry.getValue()));
            groups.add(group(
                    "display:" + entry.getKey(),
                    FieldConflictType.DISPLAY_NAME_DUPLICATE,
                    "显示名重复: " + entry.getKey(),
                    "多个字段使用相同显示名，容易让 AI 混淆业务含义。",
                    entry.getValue(),
                    evidence,
                    "确认是否同义字段；若需保留，请补充更具体显示名、注释或标签。"
            ));
        }
        return groups;
    }

    private List<FieldConflictGroup> detectSemanticDuplicates(List<Field> fields) {
        Map<String, List<Field>> bySemantic = new LinkedHashMap<>();
        for (Field field : fields) {
            for (String groupKey : semanticGroupsForField(field)) {
                bySemantic.computeIfAbsent(groupKey, ignored -> new ArrayList<>()).add(field);
            }
        }

        List<FieldConflictGroup> groups = new ArrayList<>();
        for (Map.Entry<String, List<Field>> entry : bySemantic.entrySet()) {
            List<Field> uniqueFields = dedup(entry.getValue());
            if (uniqueFields.size() <= 1) {
                continue;
            }
            SemanticGroup semanticGroup = SEMANTIC_GROUPS.get(entry.getKey());
            List<String> evidence = new ArrayList<>();
            evidence.add("语义词组命中: " + entry.getKey());
            evidence.addAll(attributeMismatchEvidence(uniqueFields));
            String action = semanticGroup != null && semanticGroup.generic()
                    ? "确认这些字段是否只是同一业务域下的相关字段；若需保留，请补充分类、标签或注释说明边界。"
                    : "确认是否应合并为一个标准字段；若需要多个字段，请补充更具体命名和别名归属。";
            groups.add(group(
                    "semantic:" + entry.getKey(),
                    FieldConflictType.SEMANTIC_DUPLICATE,
                    "语义疑似重复: " + entry.getKey(),
                    "多个字段命中同一语义词组，可能是重复标准或边界不清。",
                    uniqueFields,
                    evidence,
                    action
            ));
        }
        return groups;
    }

    private FieldConflictGroup group(
            String groupKey,
            FieldConflictType type,
            String title,
            String description,
            List<Field> fields,
            List<String> evidence,
            String suggestedAction
    ) {
        FieldConflictGroup group = new FieldConflictGroup();
        group.setGroupKey(groupKey);
        group.setConflictType(type);
        group.setTitle(title);
        group.setDescription(description);
        group.setFields(fields.stream().map(this::toConflictField).toList());
        group.setEvidence(evidence);
        group.setSuggestedAction(suggestedAction);
        group.setSeverity(severityOf(type, evidence));
        return group;
    }

    private FieldConflictSeverity severityOf(FieldConflictType type, List<String> evidence) {
        boolean hasMismatch = evidence.stream().anyMatch(item -> item.contains("不一致"));
        if (FieldConflictType.NAME_DUPLICATE.equals(type) || FieldConflictType.ALIAS_CONFLICT.equals(type)) {
            return FieldConflictSeverity.ERROR;
        }
        if (hasMismatch) {
            return FieldConflictSeverity.WARNING;
        }
        return FieldConflictType.SEMANTIC_DUPLICATE.equals(type)
                ? FieldConflictSeverity.INFO
                : FieldConflictSeverity.WARNING;
    }

    private List<String> attributeMismatchEvidence(List<Field> fields) {
        List<String> evidence = new ArrayList<>();
        addMismatchEvidence(evidence, "数据类型不一致", fields.stream().map(Field::getDataType).toList());
        addMismatchEvidence(evidence, "代码集不一致", fields.stream().map(field -> field.getCodeSetId() == null ? null : String.valueOf(field.getCodeSetId())).toList());
        addMismatchEvidence(evidence, "敏感标记不一致", fields.stream().map(field -> String.valueOf(Boolean.TRUE.equals(field.getSensitive()))).toList());
        addMismatchEvidence(evidence, "字段状态不一致", fields.stream().map(Field::getStatus).toList());
        return evidence;
    }

    private void addMismatchEvidence(List<String> evidence, String label, List<String> values) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            normalized.add(nullToEmpty(value).toLowerCase(Locale.ROOT));
        }
        if (normalized.size() > 1) {
            evidence.add(label + ": " + String.join(", ", normalized));
        }
    }

    private void buildSummary(FieldConflictReport report) {
        FieldConflictSummary summary = report.getSummary();
        summary.setConflictGroupCount(report.getGroups().size());
        Set<Long> affectedFieldIds = new LinkedHashSet<>();
        for (FieldConflictGroup group : report.getGroups()) {
            if (FieldConflictSeverity.ERROR.equals(group.getSeverity())) {
                summary.setErrorCount(summary.getErrorCount() + 1);
            } else if (FieldConflictSeverity.WARNING.equals(group.getSeverity())) {
                summary.setWarningCount(summary.getWarningCount() + 1);
            } else {
                summary.setInfoCount(summary.getInfoCount() + 1);
            }
            if (FieldConflictType.ALIAS_CONFLICT.equals(group.getConflictType())) {
                summary.setAliasConflictCount(summary.getAliasConflictCount() + 1);
            }
            if (FieldConflictType.SEMANTIC_DUPLICATE.equals(group.getConflictType())) {
                summary.setSemanticDuplicateCount(summary.getSemanticDuplicateCount() + 1);
            }
            if (group.getEvidence().stream().anyMatch(item -> item.contains("不一致"))) {
                summary.setAttributeMismatchCount(summary.getAttributeMismatchCount() + 1);
            }
            group.getFields().stream()
                    .map(FieldConflictField::getFieldId)
                    .filter(id -> id != null)
                    .forEach(affectedFieldIds::add);
        }
        summary.setAffectedFieldCount(affectedFieldIds.size());
    }

    private FieldConflictField toConflictField(Field field) {
        FieldConflictField item = new FieldConflictField();
        item.setFieldId(field.getId());
        item.setName(field.getName());
        item.setDisplayName(field.getDisplayName());
        item.setDataType(field.getDataType());
        item.setCodeSetId(field.getCodeSetId());
        item.setSensitive(field.getSensitive());
        item.setStatus(field.getStatus());
        item.setAliases(splitCsv(field.getAliases()));
        return item;
    }

    private Map<String, List<Field>> groupByNormalized(List<Field> fields, FieldValueGetter getter) {
        Map<String, List<Field>> grouped = new LinkedHashMap<>();
        for (Field field : fields) {
            String key = normalize(getter.get(field));
            if (key.isBlank()) {
                continue;
            }
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(field);
        }
        return grouped;
    }

    private List<Field> dedup(List<Field> fields) {
        Map<Long, Field> byId = new LinkedHashMap<>();
        for (Field field : fields) {
            byId.put(field.getId(), field);
        }
        return new ArrayList<>(byId.values());
    }

    private Set<String> semanticGroupsForField(Field field) {
        List<String> values = new ArrayList<>();
        values.add(field.getName());
        values.add(field.getDisplayName());
        values.add(field.getComment());
        values.add(field.getCategory());
        values.add(field.getTags());
        values.addAll(splitCsv(field.getAliases()));
        Set<String> groups = new LinkedHashSet<>();
        for (String value : values) {
            groups.addAll(semanticGroupsForText(value));
        }
        return groups;
    }

    private Set<String> semanticGroupsForText(String value) {
        Set<String> groups = new LinkedHashSet<>();
        if (value == null || value.isBlank()) {
            return groups;
        }
        String compact = normalize(value);
        for (SemanticGroup group : SEMANTIC_GROUPS.values()) {
            for (String keyword : group.keywords()) {
                String keywordCompact = normalize(keyword);
                if (!keywordCompact.isBlank() && compact.contains(keywordCompact)) {
                    groups.add(group.canonical());
                    break;
                }
            }
        }
        return groups;
    }

    private int severityRank(FieldConflictSeverity severity) {
        if (FieldConflictSeverity.ERROR.equals(severity)) {
            return 0;
        }
        if (FieldConflictSeverity.WARNING.equals(severity)) {
            return 1;
        }
        return 2;
    }

    private List<String> splitCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (String part : value.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isBlank() && !values.contains(trimmed)) {
                values.add(trimmed);
            }
        }
        return values;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{IsHan}a-z0-9]+", "");
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static Map<String, SemanticGroup> semanticGroups() {
        Map<String, SemanticGroup> groups = new LinkedHashMap<>();
        putGroup(groups, "user_id", false,
                "user_id", "userid", "uid", "account_id", "member_id",
                "用户编号", "用户id", "用户标识", "账号id", "会员编号");
        putGroup(groups, "mobile_no", false,
                "mobile_no", "mobileno", "mobile", "phone", "tel",
                "手机号", "手机号码", "手机", "联系电话", "电话");
        putGroup(groups, "amount_cent", false,
                "amount_cent", "amountcent", "pay_amount", "amount", "fee", "price",
                "付款金额", "支付金额", "订单金额", "金额", "费用", "价格");
        putGroup(groups, "id_card_no", false,
                "id_card_no", "idcardno", "sfzh", "identity_no", "id_card",
                "身份证号", "身份证", "证件号码");
        putGroup(groups, "order_no", false,
                "order_no", "orderno", "order_code", "订单号", "订单编号");
        putGroup(groups, "status", true,
                "status", "state", "状态");
        return groups;
    }

    private static void putGroup(Map<String, SemanticGroup> groups, String canonical, boolean generic,
                                 String... keywords) {
        groups.put(canonical, new SemanticGroup(canonical, Set.of(keywords), generic));
    }

    private interface FieldValueGetter {
        String get(Field field);
    }

    private record SemanticGroup(String canonical, Set<String> keywords, boolean generic) {
    }
}
