package com.dataspec.standardreuse.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.common.service.ProjectFieldNameReservationGuard;
import com.dataspec.domain.entity.Domain;
import com.dataspec.domain.repository.DomainRepository;
import com.dataspec.enumdict.entity.EnumDict;
import com.dataspec.enumdict.entity.EnumValue;
import com.dataspec.enumdict.repository.EnumDictRepository;
import com.dataspec.field.entity.Field;
import com.dataspec.field.repository.FieldRepository;
import com.dataspec.project.entity.Project;
import com.dataspec.project.service.ProjectService;
import com.dataspec.rule.entity.RuleConfig;
import com.dataspec.rule.repository.RuleConfigRepository;
import com.dataspec.security.context.DataSpecSecurityContext;
import com.dataspec.security.context.ProjectAccessGuard;
import com.dataspec.standardreuse.entity.StandardReusePack;
import com.dataspec.standardreuse.entity.StandardReusePackApplication;
import com.dataspec.standardreuse.model.StandardReusePackApplicationInfo;
import com.dataspec.standardreuse.model.StandardReusePackApplyReq;
import com.dataspec.standardreuse.model.StandardReusePackApplyResult;
import com.dataspec.standardreuse.model.StandardReusePackAssetCounts;
import com.dataspec.standardreuse.model.StandardReusePackCreateReq;
import com.dataspec.standardreuse.model.StandardReusePackDetail;
import com.dataspec.standardreuse.model.StandardReusePackDriftCounts;
import com.dataspec.standardreuse.model.StandardReusePackDriftReport;
import com.dataspec.standardreuse.model.StandardReusePackInfo;
import com.dataspec.standardreuse.model.StandardReusePackPlan;
import com.dataspec.standardreuse.model.StandardReusePackPlanCounts;
import com.dataspec.standardreuse.model.StandardReusePackPlanItem;
import com.dataspec.standardreuse.repository.StandardReusePackApplicationRepository;
import com.dataspec.standardreuse.repository.StandardReusePackRepository;
import com.dataspec.standardreuse.service.StandardReusePackService;
import com.dataspec.template.entity.Template;
import com.dataspec.template.entity.TemplateField;
import com.dataspec.template.repository.TemplateRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 标准复用包服务实现。
 */
@Service
@RequiredArgsConstructor
public class StandardReusePackServiceImpl implements StandardReusePackService {

    public static final String SOURCE_TAG_PREFIX = "pack:";

    private static final int SCHEMA_VERSION = 1;
    private static final int DRIFT_SCHEMA_VERSION = 1;
    private static final String DEFAULT_STATUS = "enabled";

    private final ProjectService projectService;
    private final DomainRepository domainRepository;
    private final FieldRepository fieldRepository;
    private final ProjectFieldNameReservationGuard fieldNameReservationGuard;
    private final EnumDictRepository enumDictRepository;
    private final RuleConfigRepository ruleConfigRepository;
    private final TemplateRepository templateRepository;
    private final StandardReusePackRepository packRepository;
    private final StandardReusePackApplicationRepository applicationRepository;
    private final ObjectMapper objectMapper;

    @Override
    public List<StandardReusePackInfo> listPacks(Long projectId) {
        ProjectAccessGuard.requireProjectAccess(projectId);
        return packRepository.findByProjectId(projectId).stream()
                .map(this::toInfo)
                .toList();
    }

    @Override
    public StandardReusePackDetail getPack(Long packId) {
        StandardReusePack pack = resolvePack(packId);
        ProjectAccessGuard.requireProjectAccess(pack.getProjectId());
        return toDetail(pack);
    }

    @Override
    @Transactional
    public StandardReusePackDetail createPack(StandardReusePackCreateReq req) {
        Long projectId = requiredProjectId(req == null ? null : req.projectId());
        Project sourceProject = projectService.getById(projectId);
        String packKey = normalizeRequired(req.packKey(), "复用包 key 不能为空");
        String packName = safeText(normalizeRequired(req.packName(), "复用包名称不能为空"));
        String basePackVersion = normalizeRequired(req.basePackVersion(), "复用包版本不能为空");
        String description = safeText(normalizeOptional(req.description()));
        if (packRepository.existsByProjectIdAndKeyAndVersion(projectId, packKey, basePackVersion)) {
            throw new BizException("标准复用包版本已存在: " + packKey + "@" + basePackVersion);
        }

        PackPayload payload = buildPayload(sourceProject, packKey, packName, basePackVersion, description);
        String payloadJson = toJson(payload);
        assertNoRawSensitivePayload(payloadJson);
        String packageHash = sha256(payloadJson);
        StandardReusePackAssetCounts counts = countAssets(payload);

        StandardReusePack pack = new StandardReusePack();
        pack.setProjectId(projectId);
        pack.setSourceProjectName(safeText(sourceProject.getName()));
        pack.setPackKey(packKey);
        pack.setPackName(packName);
        pack.setBasePackVersion(basePackVersion);
        pack.setDescription(description);
        pack.setPackageHash(packageHash);
        pack.setPayloadJson(payloadJson);
        pack.setAssetCountsJson(toJson(counts));
        packRepository.insert(pack);
        return toDetail(pack);
    }

    @Override
    public StandardReusePackPlan previewApply(StandardReusePackApplyReq req) {
        ApplyContext context = applyContext(req);
        return buildPlan(context.pack, context.payload, context.targetProjectId);
    }

    @Override
    @Transactional
    public StandardReusePackApplyResult applyPack(StandardReusePackApplyReq req) {
        ApplyContext context = applyContext(req);
        StandardReusePackPlan plan = buildPlan(context.pack, context.payload, context.targetProjectId);
        if (!Boolean.TRUE.equals(plan.canApply())) {
            throw new BizException("标准复用包存在阻塞项，不能应用");
        }
        ApplyAccumulator acc = new ApplyAccumulator();
        applyPayload(context.payload, context.targetProjectId, sourceTag(context.payload), acc);
        // 应用记录会进入 AI Context manifest，必须保存写入后的漂移，避免首次应用成功后仍显示 MISSING。
        StandardReusePackDriftReport postApplyDriftReport = buildDriftReport(
                context.pack,
                context.payload,
                context.targetProjectId);
        StandardReusePackApplication application = saveApplication(
                context.pack,
                context.payload,
                context.targetProjectId,
                acc,
                postApplyDriftReport);
        return new StandardReusePackApplyResult(plan, toApplicationInfo(application));
    }

    @Override
    public List<StandardReusePackApplicationInfo> listApplications(Long projectId) {
        ProjectAccessGuard.requireProjectAccess(projectId);
        return applicationRepository.findByProjectId(projectId).stream()
                .map(this::toApplicationInfo)
                .toList();
    }

    @Override
    public StandardReusePackDriftReport driftReport(Long packId, Long targetProjectId) {
        StandardReusePack pack = resolvePack(packId);
        ProjectAccessGuard.requireProjectAccess(pack.getProjectId());
        ProjectAccessGuard.requireProjectAccess(targetProjectId);
        PackPayload payload = validatePayload(pack);
        projectService.getById(targetProjectId);
        return buildDriftReport(pack, payload, targetProjectId);
    }

    private ApplyContext applyContext(StandardReusePackApplyReq req) {
        if (req == null) {
            throw new BizException("复用包应用请求不能为空");
        }
        Long targetProjectId = requiredProjectId(req.targetProjectId());
        StandardReusePack pack = resolvePack(req.packId());
        ProjectAccessGuard.requireProjectAccess(pack.getProjectId());
        ProjectAccessGuard.requireProjectAccess(targetProjectId);
        projectService.getById(targetProjectId);
        return new ApplyContext(pack, validatePayload(pack), targetProjectId);
    }

    private PackPayload buildPayload(Project sourceProject, String packKey, String packName, String basePackVersion, String description) {
        Long projectId = sourceProject.getId();
        Map<Long, String> domainCodes = domainRepository.findByProjectId(projectId).stream()
                .collect(Collectors.toMap(Domain::getId, Domain::getCode, (a, b) -> a));
        Map<Long, String> enumCodes = enumDictRepository.findDictsByProjectId(projectId).stream()
                .collect(Collectors.toMap(EnumDict::getId, EnumDict::getCode, (a, b) -> a));
        Map<Long, String> fieldNames = fieldRepository.findAllByProjectId(projectId).stream()
                .collect(Collectors.toMap(Field::getId, Field::getName, (a, b) -> a));

        List<PackDomain> domains = domainRepository.findByProjectId(projectId).stream()
                .map(domain -> new PackDomain(safeText(domain.getCode()), safeText(domain.getName()), safeText(domain.getDescription())))
                .sorted(Comparator.comparing(PackDomain::code, Comparator.nullsLast(String::compareTo)))
                .toList();
        List<PackEnum> enums = enumDictRepository.findDictsByProjectId(projectId).stream()
                .map(dict -> new PackEnum(
                        safeText(dict.getCode()),
                        safeText(dict.getName()),
                        safeText(dict.getDescription()),
                        safeText(dict.getValueType()),
                        enumDictRepository.findValuesByEnumId(dict.getId()).stream()
                                .map(value -> new PackEnumValue(safeText(value.getValue()), safeText(value.getLabel()), value.getSortOrder()))
                                .sorted(Comparator.comparing(PackEnumValue::sortOrder, Comparator.nullsLast(Integer::compareTo))
                                        .thenComparing(PackEnumValue::value, Comparator.nullsLast(String::compareTo)))
                                .toList()))
                .sorted(Comparator.comparing(PackEnum::code, Comparator.nullsLast(String::compareTo)))
                .toList();
        List<PackField> fields = fieldRepository.findAllByProjectId(projectId).stream()
                .map(field -> toPackField(field, domainCodes, enumCodes))
                .sorted(Comparator.comparing(PackField::name, Comparator.nullsLast(String::compareTo)))
                .toList();
        List<PackRule> rules = ruleConfigRepository.findByProjectId(projectId).stream()
                .map(rule -> new PackRule(
                        safeText(rule.getRuleCode()),
                        safeText(rule.getRuleName()),
                        safeText(rule.getSeverity()),
                        rule.getEnabled(),
                        safeJsonText(rule.getParamsJson())))
                .sorted(Comparator.comparing(PackRule::ruleCode, Comparator.nullsLast(String::compareTo)))
                .toList();
        List<PackTemplate> templates = templateRepository.findByProjectId(projectId).stream()
                .map(template -> new PackTemplate(
                        safeText(template.getName()),
                        safeText(template.getDescription()),
                        safeText(template.getTablePrefix()),
                        templateRepository.findFieldsByTemplateId(template.getId()).stream()
                                .map(field -> toPackTemplateField(field, fieldNames))
                                .sorted(Comparator.comparing(PackTemplateField::sortOrder, Comparator.nullsLast(Integer::compareTo))
                                        .thenComparing(PackTemplateField::naturalName, Comparator.nullsLast(String::compareTo)))
                                .toList()))
                .sorted(Comparator.comparing(PackTemplate::name, Comparator.nullsLast(String::compareTo)))
                .toList();
        return new PackPayload(
                SCHEMA_VERSION,
                sourceProject.getId(),
                safeText(sourceProject.getName()),
                packKey,
                packName,
                basePackVersion,
                description,
                domains,
                enums,
                fields,
                rules,
                templates);
    }

    private PackField toPackField(Field field, Map<Long, String> domainCodes, Map<Long, String> enumCodes) {
        boolean sensitiveField = Boolean.TRUE.equals(field.getSensitive());
        return new PackField(
                safeText(field.getName()),
                safeText(field.getDisplayName()),
                safeText(field.getDataType()),
                field.getLength(),
                field.getPrecisionVal(),
                field.getScaleVal(),
                field.getNullable(),
                safeFieldValue(sensitiveField, field.getDefaultValue()),
                safeText(field.getComment()),
                safeText(domainCodes.get(field.getDomainId())),
                safeText(field.getTags()),
                safeText(field.getAliases()),
                safeText(field.getCategory()),
                safeText(enumCodes.get(field.getCodeSetId())),
                field.getSensitive(),
                textOrDefault(safeText(field.getStatus()), DEFAULT_STATUS),
                safeText(field.getReplacementReason()),
                safeFieldValue(sensitiveField, field.getExampleValue()),
                safeText(field.getFormatType()),
                safeText(field.getFormatPattern()),
                safeText(field.getFormatUnit()),
                safeText(field.getFormatPrecision()),
                safeText(field.getFormatTimezone()),
                safeText(field.getFormatNullPolicy()),
                safeFieldJsonValue(sensitiveField, field.getValidExamplesJson()),
                safeFieldJsonValue(sensitiveField, field.getInvalidExamplesJson()),
                safeText(field.getFormatNotes()),
                safeText(field.getPreferredUseCases()),
                safeText(field.getAvoidWhen()),
                safeText(field.getJoinHints()),
                safeText(field.getDefaultFilters()),
                safeText(field.getAggregationHints()),
                safeText(field.getReplacementGuidance()),
                safeText(field.getMisuseExamples()));
    }

    private PackTemplateField toPackTemplateField(TemplateField field, Map<Long, String> fieldNames) {
        return new PackTemplateField(
                safeText(fieldNames.get(field.getFieldId())),
                safeText(field.getName()),
                safeText(field.getDataType()),
                field.getNullable(),
                safeText(field.getDefaultValue()),
                safeText(field.getComment()),
                field.getSortOrder(),
                field.getIsRequired());
    }

    private StandardReusePackPlan buildPlan(StandardReusePack pack, PackPayload payload, Long targetProjectId) {
        StandardReusePackDriftReport driftReport = buildDriftReport(pack, payload, targetProjectId);
        List<StandardReusePackPlanItem> planItems = driftReport.items().stream()
                .map(this::toPlanItem)
                .toList();
        StandardReusePackPlanCounts counts = countPlan(planItems);
        List<String> warnings = new ArrayList<>();
        if (counts.drifted() > 0 || counts.overridden() > 0) {
            warnings.add("目标项目存在本地覆盖或漂移项，第一版不会自动覆盖这些资产");
        }
        if (counts.blocked() > 0) {
            warnings.add("复用包存在缺少自然键的资产，请修正后再应用");
        }
        return new StandardReusePackPlan(
                "standard-reuse-pack-plan",
                SCHEMA_VERSION,
                pack.getId(),
                targetProjectId,
                payload.packKey(),
                payload.basePackVersion(),
                counts.blocked() == 0,
                new StandardReusePackPlanCounts(
                        counts.created(),
                        counts.skipped(),
                        counts.overridden(),
                        counts.drifted(),
                        counts.blocked(),
                        warnings.size()),
                planItems,
                warnings,
                driftReport);
    }

    private StandardReusePackPlanItem toPlanItem(StandardReusePackPlanItem driftItem) {
        return switch (driftItem.action()) {
            case "MISSING" -> new StandardReusePackPlanItem(driftItem.assetType(), driftItem.key(), "CREATE", "目标项目缺失，将创建");
            case "MATCHED" -> new StandardReusePackPlanItem(driftItem.assetType(), driftItem.key(), "SKIP", "目标项目已存在且内容一致");
            default -> driftItem;
        };
    }

    private StandardReusePackPlanCounts countPlan(List<StandardReusePackPlanItem> items) {
        Map<String, Long> counts = items.stream()
                .collect(Collectors.groupingBy(StandardReusePackPlanItem::action, Collectors.counting()));
        int driftWarnings = count(counts, "DRIFTED") + count(counts, "OVERRIDDEN");
        return new StandardReusePackPlanCounts(
                count(counts, "CREATE"),
                count(counts, "SKIP"),
                count(counts, "OVERRIDDEN"),
                count(counts, "DRIFTED"),
                count(counts, "BLOCKED"),
                driftWarnings);
    }

    private StandardReusePackDriftReport buildDriftReport(StandardReusePack pack, PackPayload payload, Long targetProjectId) {
        TargetCatalog target = loadTargetCatalog(targetProjectId);
        DriftAccumulator acc = new DriftAccumulator();
        for (PackDomain item : safeList(payload.domains())) {
            acc.add(compareDomain(item, target.domainsByCode));
        }
        for (PackEnum item : safeList(payload.enums())) {
            acc.add(compareEnum(item, target.enumsByCode, target.enumValuesByEnumCode));
        }
        for (PackField item : safeList(payload.fields())) {
            acc.add(compareField(item, payload, target));
        }
        for (PackRule item : safeList(payload.rules())) {
            acc.add(compareRule(item, target.rulesByCode));
        }
        for (PackTemplate item : safeList(payload.templates())) {
            acc.add(compareTemplate(item, target.templatesByName, target.templateFieldsByTemplateName, target.fieldNamesById));
        }
        return new StandardReusePackDriftReport(
                DRIFT_SCHEMA_VERSION,
                pack.getId(),
                targetProjectId,
                payload.packKey(),
                payload.basePackVersion(),
                acc.counts,
                acc.items);
    }

    private TargetCatalog loadTargetCatalog(Long projectId) {
        List<Domain> domains = domainRepository.findByProjectId(projectId);
        List<EnumDict> enums = enumDictRepository.findDictsByProjectId(projectId);
        List<Field> fields = fieldRepository.findAllByProjectId(projectId);
        List<Template> templates = templateRepository.findByProjectId(projectId);
        Map<String, List<EnumValue>> enumValues = new LinkedHashMap<>();
        for (EnumDict dict : enums) {
            enumValues.put(dict.getCode(), enumDictRepository.findValuesByEnumId(dict.getId()));
        }
        Map<String, List<TemplateField>> templateFields = new LinkedHashMap<>();
        for (Template template : templates) {
            templateFields.put(template.getName(), templateRepository.findFieldsByTemplateId(template.getId()));
        }
        return new TargetCatalog(
                toMap(domains, Domain::getCode),
                toMap(enums, EnumDict::getCode),
                enumValues,
                toMap(fields, Field::getName),
                fields.stream().filter(field -> field.getId() != null)
                        .collect(Collectors.toMap(Field::getId, Field::getName, (a, b) -> a, LinkedHashMap::new)),
                toMap(ruleConfigRepository.findByProjectId(projectId), RuleConfig::getRuleCode),
                toMap(templates, Template::getName),
                templateFields);
    }

    private StandardReusePackPlanItem compareDomain(PackDomain pack, Map<String, Domain> target) {
        if (isBlank(pack.code())) {
            return new StandardReusePackPlanItem("domain", "", "BLOCKED", "数据域缺少编码");
        }
        Domain existing = target.get(pack.code());
        if (existing == null) {
            return new StandardReusePackPlanItem("domain", pack.code(), "MISSING", "目标项目缺少数据域");
        }
        return Objects.equals(pack.name(), existing.getName())
                && Objects.equals(pack.description(), existing.getDescription())
                ? new StandardReusePackPlanItem("domain", pack.code(), "MATCHED", "数据域内容一致")
                : new StandardReusePackPlanItem("domain", pack.code(), "OVERRIDDEN", "目标项目数据域存在本地覆盖或内容不同");
    }

    private StandardReusePackPlanItem compareEnum(
            PackEnum pack,
            Map<String, EnumDict> target,
            Map<String, List<EnumValue>> targetValues
    ) {
        if (isBlank(pack.code())) {
            return new StandardReusePackPlanItem("enum_dict", "", "BLOCKED", "枚举缺少编码");
        }
        EnumDict existing = target.get(pack.code());
        if (existing == null) {
            return new StandardReusePackPlanItem("enum_dict", pack.code(), "MISSING", "目标项目缺少枚举");
        }
        List<PackEnumValue> packValues = sortedEnumValues(pack.values());
        List<PackEnumValue> currentValues = safeList(targetValues.get(pack.code())).stream()
                .map(value -> new PackEnumValue(value.getValue(), value.getLabel(), value.getSortOrder()))
                .sorted(enumValueComparator())
                .toList();
        boolean same = Objects.equals(pack.name(), existing.getName())
                && Objects.equals(pack.description(), existing.getDescription())
                && Objects.equals(pack.valueType(), existing.getValueType())
                && Objects.equals(packValues, currentValues);
        return same
                ? new StandardReusePackPlanItem("enum_dict", pack.code(), "MATCHED", "枚举内容一致")
                : new StandardReusePackPlanItem("enum_dict", pack.code(), "OVERRIDDEN", "目标项目枚举存在本地覆盖或内容不同");
    }

    private StandardReusePackPlanItem compareField(PackField pack, PackPayload payload, TargetCatalog target) {
        if (isBlank(pack.name())) {
            return new StandardReusePackPlanItem("field", "", "BLOCKED", "字段缺少名称");
        }
        Field existing = target.fieldsByName.get(pack.name());
        if (existing == null) {
            return new StandardReusePackPlanItem("field", pack.name(), "MISSING", "目标项目缺少字段");
        }
        String domainCode = Optional.ofNullable(existing.getDomainId()).map(target.domainCodesById()::get).orElse(null);
        String enumCode = Optional.ofNullable(existing.getCodeSetId()).map(target.enumCodesById()::get).orElse(null);
        boolean same = Objects.equals(pack.displayName(), existing.getDisplayName())
                && Objects.equals(pack.dataType(), existing.getDataType())
                && Objects.equals(pack.length(), existing.getLength())
                && Objects.equals(pack.precisionVal(), existing.getPrecisionVal())
                && Objects.equals(pack.scaleVal(), existing.getScaleVal())
                && Objects.equals(pack.nullable(), existing.getNullable())
                && Objects.equals(pack.defaultValue(), existing.getDefaultValue())
                && Objects.equals(pack.comment(), existing.getComment())
                && Objects.equals(pack.domainCode(), domainCode)
                && Objects.equals(normalizedBusinessTags(pack.tags()), normalizedBusinessTags(existing.getTags()))
                && Objects.equals(pack.aliases(), existing.getAliases())
                && Objects.equals(pack.codeSetCode(), enumCode)
                && Objects.equals(pack.category(), existing.getCategory())
                && Objects.equals(pack.sensitive(), existing.getSensitive())
                && Objects.equals(pack.status(), existing.getStatus())
                && Objects.equals(pack.replacementReason(), existing.getReplacementReason())
                && Objects.equals(pack.exampleValue(), existing.getExampleValue())
                && Objects.equals(pack.formatType(), existing.getFormatType())
                && Objects.equals(pack.formatPattern(), existing.getFormatPattern())
                && Objects.equals(pack.formatUnit(), existing.getFormatUnit())
                && Objects.equals(pack.formatPrecision(), existing.getFormatPrecision())
                && Objects.equals(pack.formatTimezone(), existing.getFormatTimezone())
                && Objects.equals(pack.formatNullPolicy(), existing.getFormatNullPolicy())
                && Objects.equals(pack.validExamplesJson(), existing.getValidExamplesJson())
                && Objects.equals(pack.invalidExamplesJson(), existing.getInvalidExamplesJson())
                && Objects.equals(pack.formatNotes(), existing.getFormatNotes())
                && Objects.equals(pack.preferredUseCases(), existing.getPreferredUseCases())
                && Objects.equals(pack.avoidWhen(), existing.getAvoidWhen())
                && Objects.equals(pack.joinHints(), existing.getJoinHints())
                && Objects.equals(pack.defaultFilters(), existing.getDefaultFilters())
                && Objects.equals(pack.aggregationHints(), existing.getAggregationHints())
                && Objects.equals(pack.replacementGuidance(), existing.getReplacementGuidance())
                && Objects.equals(pack.misuseExamples(), existing.getMisuseExamples());
        return same
                ? new StandardReusePackPlanItem("field", pack.name(), "MATCHED", "字段内容一致")
                : new StandardReusePackPlanItem(
                        "field",
                        pack.name(),
                        hasPackSourceMarker(existing.getTags(), payload.packKey()) ? "DRIFTED" : "OVERRIDDEN",
                        "目标项目字段存在局部覆盖或内容不同");
    }

    private StandardReusePackPlanItem compareRule(PackRule pack, Map<String, RuleConfig> target) {
        if (isBlank(pack.ruleCode())) {
            return new StandardReusePackPlanItem("rule", "", "BLOCKED", "规则缺少编码");
        }
        RuleConfig existing = target.get(pack.ruleCode());
        if (existing == null) {
            return new StandardReusePackPlanItem("rule", pack.ruleCode(), "MISSING", "目标项目缺少规则配置");
        }
        boolean same = Objects.equals(pack.ruleName(), existing.getRuleName())
                && Objects.equals(pack.severity(), existing.getSeverity())
                && Objects.equals(pack.enabled(), existing.getEnabled())
                && Objects.equals(pack.paramsJson(), existing.getParamsJson());
        return same
                ? new StandardReusePackPlanItem("rule", pack.ruleCode(), "MATCHED", "规则内容一致")
                : new StandardReusePackPlanItem("rule", pack.ruleCode(), "OVERRIDDEN", "目标项目规则存在本地覆盖或内容不同");
    }

    private StandardReusePackPlanItem compareTemplate(
            PackTemplate pack,
            Map<String, Template> target,
            Map<String, List<TemplateField>> targetFields,
            Map<Long, String> fieldNamesById
    ) {
        if (isBlank(pack.name())) {
            return new StandardReusePackPlanItem("template", "", "BLOCKED", "模板缺少名称");
        }
        Template existing = target.get(pack.name());
        if (existing == null) {
            return new StandardReusePackPlanItem("template", pack.name(), "MISSING", "目标项目缺少模板");
        }
        List<PackTemplateField> packFields = sortedTemplateFields(pack.fields());
        List<PackTemplateField> currentFields = safeList(targetFields.get(pack.name())).stream()
                .map(field -> new PackTemplateField(
                        field.getFieldId() == null ? null : fieldNamesById.get(field.getFieldId()),
                        field.getName(),
                        field.getDataType(),
                        field.getNullable(),
                        field.getDefaultValue(),
                        field.getComment(),
                        field.getSortOrder(),
                        field.getIsRequired()))
                .sorted(templateFieldComparator())
                .toList();
        boolean same = Objects.equals(pack.description(), existing.getDescription())
                && Objects.equals(pack.tablePrefix(), existing.getTablePrefix())
                && Objects.equals(packFields, currentFields);
        return same
                ? new StandardReusePackPlanItem("template", pack.name(), "MATCHED", "模板内容一致")
                : new StandardReusePackPlanItem("template", pack.name(), "OVERRIDDEN", "目标项目模板存在本地覆盖或内容不同");
    }

    private void applyPayload(PackPayload payload, Long targetProjectId, String sourceTag, ApplyAccumulator acc) {
        Map<String, Domain> domainsByCode = domainRepository.findByProjectId(targetProjectId).stream()
                .collect(toNaturalMap(Domain::getCode));
        Map<String, EnumDict> enumsByCode = enumDictRepository.findDictsByProjectId(targetProjectId).stream()
                .collect(toNaturalMap(EnumDict::getCode));
        Map<String, Field> fieldsByName = fieldRepository.findAllByProjectId(targetProjectId).stream()
                .collect(toNaturalMap(Field::getName));
        Map<String, RuleConfig> rulesByCode = ruleConfigRepository.findByProjectId(targetProjectId).stream()
                .collect(toNaturalMap(RuleConfig::getRuleCode));
        Map<String, Template> templatesByName = templateRepository.findByProjectId(targetProjectId).stream()
                .collect(toNaturalMap(Template::getName));

        applyDomains(payload, targetProjectId, domainsByCode, acc);
        applyEnums(payload, targetProjectId, enumsByCode, acc);
        applyFields(payload, targetProjectId, domainsByCode, enumsByCode, fieldsByName, sourceTag, acc);
        applyRules(payload, targetProjectId, rulesByCode, acc);
        applyTemplates(payload, targetProjectId, fieldsByName, templatesByName, acc);
    }

    private void applyDomains(PackPayload payload, Long targetProjectId, Map<String, Domain> domainsByCode, ApplyAccumulator acc) {
        for (PackDomain pack : safeList(payload.domains())) {
            if (isBlank(pack.code())) {
                continue;
            }
            if (domainsByCode.containsKey(pack.code())) {
                acc.skipped = acc.skipped.plusDomains(1);
                continue;
            }
            Domain domain = new Domain();
            domain.setProjectId(targetProjectId);
            domain.setCode(pack.code());
            domain.setName(pack.name());
            domain.setDescription(pack.description());
            domainRepository.insert(domain);
            domainsByCode.put(domain.getCode(), domain);
            acc.created = acc.created.plusDomains(1);
        }
    }

    private void applyEnums(PackPayload payload, Long targetProjectId, Map<String, EnumDict> enumsByCode, ApplyAccumulator acc) {
        for (PackEnum pack : safeList(payload.enums())) {
            if (isBlank(pack.code())) {
                continue;
            }
            EnumDict dict = enumsByCode.get(pack.code());
            if (dict == null) {
                dict = new EnumDict();
                dict.setProjectId(targetProjectId);
                dict.setCode(pack.code());
                dict.setName(pack.name());
                dict.setDescription(pack.description());
                dict.setValueType(textOrDefault(pack.valueType(), "string"));
                enumDictRepository.insertDict(dict);
                enumsByCode.put(dict.getCode(), dict);
                acc.created = acc.created.plusEnums(1);
            } else {
                acc.skipped = acc.skipped.plusEnums(1);
            }
            for (PackEnumValue value : safeList(pack.values())) {
                if (isBlank(value.value()) || enumDictRepository.existsValueByEnumIdAndValue(dict.getId(), value.value())) {
                    acc.skipped = acc.skipped.plusEnumValues(1);
                    continue;
                }
                EnumValue enumValue = new EnumValue();
                enumValue.setEnumId(dict.getId());
                enumValue.setValue(value.value());
                enumValue.setLabel(value.label());
                enumValue.setSortOrder(value.sortOrder());
                enumDictRepository.insertValue(enumValue);
                acc.created = acc.created.plusEnumValues(1);
            }
        }
    }

    private void applyFields(
            PackPayload payload,
            Long targetProjectId,
            Map<String, Domain> domainsByCode,
            Map<String, EnumDict> enumsByCode,
            Map<String, Field> fieldsByName,
            String sourceTag,
            ApplyAccumulator acc
    ) {
        List<String> namesToCreate = safeList(payload.fields()).stream()
                .map(PackField::name)
                .filter(name -> !isBlank(name))
                .filter(name -> !fieldsByName.containsKey(name))
                .toList();
        fieldNameReservationGuard.reserveAll(
                targetProjectId,
                namesToCreate);
        fieldRepository.findByNamesInProject(namesToCreate, targetProjectId)
                .forEach(field -> fieldsByName.put(field.getName(), field));
        for (PackField pack : safeList(payload.fields())) {
            if (isBlank(pack.name())) {
                continue;
            }
            if (fieldsByName.containsKey(pack.name())) {
                acc.skipped = acc.skipped.plusFields(1);
                continue;
            }
            Field field = toField(pack, targetProjectId, domainsByCode, enumsByCode, sourceTag);
            fieldRepository.insert(field);
            fieldsByName.put(field.getName(), field);
            acc.created = acc.created.plusFields(1);
        }
    }

    private Field toField(
            PackField pack,
            Long targetProjectId,
            Map<String, Domain> domainsByCode,
            Map<String, EnumDict> enumsByCode,
            String sourceTag
    ) {
        Field field = new Field();
        field.setProjectId(targetProjectId);
        field.setName(pack.name());
        field.setDisplayName(pack.displayName());
        field.setDataType(pack.dataType());
        field.setLength(pack.length());
        field.setPrecisionVal(pack.precisionVal());
        field.setScaleVal(pack.scaleVal());
        field.setNullable(pack.nullable());
        field.setDefaultValue(pack.defaultValue());
        field.setComment(pack.comment());
        field.setDomainId(Optional.ofNullable(domainsByCode.get(pack.domainCode())).map(Domain::getId).orElse(null));
        field.setTags(appendTag(pack.tags(), sourceTag));
        field.setAliases(pack.aliases());
        field.setCategory(pack.category());
        field.setCodeSetId(Optional.ofNullable(enumsByCode.get(pack.codeSetCode())).map(EnumDict::getId).orElse(null));
        field.setSensitive(pack.sensitive());
        field.setStatus(textOrDefault(pack.status(), DEFAULT_STATUS));
        field.setReplacementReason(pack.replacementReason());
        field.setExampleValue(pack.exampleValue());
        field.setFormatType(pack.formatType());
        field.setFormatPattern(pack.formatPattern());
        field.setFormatUnit(pack.formatUnit());
        field.setFormatPrecision(pack.formatPrecision());
        field.setFormatTimezone(pack.formatTimezone());
        field.setFormatNullPolicy(pack.formatNullPolicy());
        field.setValidExamplesJson(pack.validExamplesJson());
        field.setInvalidExamplesJson(pack.invalidExamplesJson());
        field.setFormatNotes(pack.formatNotes());
        field.setPreferredUseCases(pack.preferredUseCases());
        field.setAvoidWhen(pack.avoidWhen());
        field.setJoinHints(pack.joinHints());
        field.setDefaultFilters(pack.defaultFilters());
        field.setAggregationHints(pack.aggregationHints());
        field.setReplacementGuidance(pack.replacementGuidance());
        field.setMisuseExamples(pack.misuseExamples());
        return field;
    }

    private void applyRules(PackPayload payload, Long targetProjectId, Map<String, RuleConfig> rulesByCode, ApplyAccumulator acc) {
        for (PackRule pack : safeList(payload.rules())) {
            if (isBlank(pack.ruleCode())) {
                continue;
            }
            if (rulesByCode.containsKey(pack.ruleCode())) {
                acc.skipped = acc.skipped.plusRules(1);
                continue;
            }
            RuleConfig rule = new RuleConfig();
            rule.setProjectId(targetProjectId);
            rule.setRuleCode(pack.ruleCode());
            rule.setRuleName(pack.ruleName());
            rule.setSeverity(pack.severity());
            rule.setEnabled(pack.enabled());
            rule.setParamsJson(pack.paramsJson());
            ruleConfigRepository.insert(rule);
            rulesByCode.put(rule.getRuleCode(), rule);
            acc.created = acc.created.plusRules(1);
        }
    }

    private void applyTemplates(
            PackPayload payload,
            Long targetProjectId,
            Map<String, Field> fieldsByName,
            Map<String, Template> templatesByName,
            ApplyAccumulator acc
    ) {
        for (PackTemplate pack : safeList(payload.templates())) {
            if (isBlank(pack.name())) {
                continue;
            }
            if (templatesByName.containsKey(pack.name())) {
                acc.skipped = acc.skipped.plusTemplates(1);
                continue;
            }
            Template template = new Template();
            template.setProjectId(targetProjectId);
            template.setName(pack.name());
            template.setDescription(pack.description());
            template.setTablePrefix(pack.tablePrefix());
            templateRepository.insert(template);
            templatesByName.put(template.getName(), template);
            acc.created = acc.created.plusTemplates(1);
            for (PackTemplateField item : safeList(pack.fields())) {
                TemplateField field = new TemplateField();
                field.setTemplateId(template.getId());
                field.setFieldId(Optional.ofNullable(fieldsByName.get(item.fieldName())).map(Field::getId).orElse(null));
                field.setName(item.name());
                field.setDataType(item.dataType());
                field.setNullable(item.nullable());
                field.setDefaultValue(item.defaultValue());
                field.setComment(item.comment());
                field.setSortOrder(item.sortOrder());
                field.setIsRequired(item.isRequired());
                templateRepository.insertField(field);
                acc.created = acc.created.plusTemplateFields(1);
            }
        }
    }

    private StandardReusePackApplication saveApplication(
            StandardReusePack pack,
            PackPayload payload,
            Long targetProjectId,
            ApplyAccumulator acc,
            StandardReusePackDriftReport driftReport
    ) {
        StandardReusePackApplication application = new StandardReusePackApplication();
        application.setProjectId(targetProjectId);
        application.setPackId(pack.getId());
        application.setPackKey(payload.packKey());
        application.setPackName(payload.packName());
        application.setBasePackVersion(payload.basePackVersion());
        application.setPackageHash(pack.getPackageHash());
        application.setSourceProjectId(pack.getProjectId());
        application.setSourceProjectName(pack.getSourceProjectName());
        application.setCreatedCountsJson(toJson(acc.created));
        application.setSkippedCountsJson(toJson(acc.skipped));
        application.setDriftCountsJson(toJson(driftReport.counts()));
        application.setDriftReportJson(toJson(driftReport));
        application.setOperatorName(DataSpecSecurityContext.currentOperator());
        application.setAppliedAt(LocalDateTime.now());
        applicationRepository.insert(application);
        return application;
    }

    private StandardReusePackInfo toInfo(StandardReusePack pack) {
        return new StandardReusePackInfo(
                pack.getId(),
                pack.getProjectId(),
                pack.getSourceProjectName(),
                pack.getPackKey(),
                pack.getPackName(),
                pack.getBasePackVersion(),
                pack.getDescription(),
                pack.getPackageHash(),
                readValue(pack.getAssetCountsJson(), StandardReusePackAssetCounts.class, StandardReusePackAssetCounts.empty()),
                pack.getCreatedAt());
    }

    private StandardReusePackDetail toDetail(StandardReusePack pack) {
        return new StandardReusePackDetail(toInfo(pack), pack.getPayloadJson());
    }

    private StandardReusePackApplicationInfo toApplicationInfo(StandardReusePackApplication application) {
        return new StandardReusePackApplicationInfo(
                application.getId(),
                application.getProjectId(),
                application.getPackId(),
                application.getPackKey(),
                application.getPackName(),
                application.getBasePackVersion(),
                application.getPackageHash(),
                application.getSourceProjectId(),
                application.getSourceProjectName(),
                readValue(application.getCreatedCountsJson(), StandardReusePackAssetCounts.class, StandardReusePackAssetCounts.empty()),
                readValue(application.getSkippedCountsJson(), StandardReusePackAssetCounts.class, StandardReusePackAssetCounts.empty()),
                readValue(application.getDriftCountsJson(), StandardReusePackDriftCounts.class, StandardReusePackDriftCounts.empty()),
                application.getAppliedAt());
    }

    private PackPayload validatePayload(StandardReusePack pack) {
        PackPayload payload = readValue(pack.getPayloadJson(), PackPayload.class, null);
        if (payload == null) {
            throw new BizException("标准复用包 payload 缺失");
        }
        if (payload.schemaVersion() != SCHEMA_VERSION) {
            throw new BizException("不支持的标准复用包 schemaVersion: " + payload.schemaVersion());
        }
        String expected = sha256(pack.getPayloadJson());
        if (!Objects.equals(expected, pack.getPackageHash())) {
            throw new BizException("标准复用包 packageHash 校验失败");
        }
        return payload;
    }

    private StandardReusePack resolvePack(Long packId) {
        if (packId == null) {
            throw new BizException("复用包 ID 不能为空");
        }
        return packRepository.findById(packId)
                .orElseThrow(() -> new BizException(404, "标准复用包不存在: " + packId));
    }

    private StandardReusePackAssetCounts countAssets(PackPayload payload) {
        int enumValues = safeList(payload.enums()).stream().mapToInt(item -> safeList(item.values()).size()).sum();
        int templateFields = safeList(payload.templates()).stream().mapToInt(item -> safeList(item.fields()).size()).sum();
        return new StandardReusePackAssetCounts(
                safeList(payload.domains()).size(),
                safeList(payload.fields()).size(),
                safeList(payload.enums()).size(),
                enumValues,
                safeList(payload.rules()).size(),
                safeList(payload.templates()).size(),
                templateFields);
    }

    private String sourceTag(PackPayload payload) {
        return SOURCE_TAG_PREFIX + payload.packKey() + "@" + payload.basePackVersion();
    }

    private String appendTag(String tags, String sourceTag) {
        if (isBlank(sourceTag)) {
            return tags;
        }
        if (isBlank(tags)) {
            return sourceTag;
        }
        List<String> parts = new ArrayList<>();
        for (String part : tags.split("[,，]")) {
            String trimmed = part.trim();
            if (!trimmed.isBlank() && !parts.contains(trimmed)) {
                parts.add(trimmed);
            }
        }
        if (!parts.contains(sourceTag)) {
            parts.add(sourceTag);
        }
        return String.join(",", parts);
    }

    private String safeText(String value) {
        return SensitiveDataSanitizer.redactText(value);
    }

    private String safeJsonText(String value) {
        if (isBlank(value)) {
            return value;
        }
        try {
            Object raw = objectMapper.readValue(value, Object.class);
            return objectMapper.writeValueAsString(SensitiveDataSanitizer.sanitizeValue(raw));
        } catch (Exception e) {
            return safeText(value);
        }
    }

    private String safeFieldValue(boolean sensitiveField, String value) {
        // 敏感字段的默认值和样例可能是可复制凭据或个人数据；复用包只携带结构约束，不携带原始值。
        return sensitiveField ? null : safeText(value);
    }

    private String safeFieldJsonValue(boolean sensitiveField, String value) {
        return sensitiveField ? null : safeJsonText(value);
    }

    private void assertNoRawSensitivePayload(String payloadJson) {
        try {
            if (SensitiveDataSanitizer.containsSensitiveKeyOrValue(objectMapper.readTree(payloadJson))) {
                throw new BizException("标准复用包包含敏感信息，请先脱敏后再创建");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            if (SensitiveDataSanitizer.containsSensitiveText(payloadJson)) {
                throw new BizException("标准复用包包含敏感信息，请先脱敏后再创建");
            }
        }
    }

    private List<PackEnumValue> sortedEnumValues(List<PackEnumValue> values) {
        return safeList(values).stream()
                .sorted(enumValueComparator())
                .toList();
    }

    private Comparator<PackEnumValue> enumValueComparator() {
        return Comparator.comparing(PackEnumValue::sortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(PackEnumValue::value, Comparator.nullsLast(String::compareTo))
                .thenComparing(PackEnumValue::label, Comparator.nullsLast(String::compareTo));
    }

    private List<PackTemplateField> sortedTemplateFields(List<PackTemplateField> fields) {
        return safeList(fields).stream()
                .sorted(templateFieldComparator())
                .toList();
    }

    private Comparator<PackTemplateField> templateFieldComparator() {
        return Comparator.comparing(PackTemplateField::sortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(PackTemplateField::naturalName, Comparator.nullsLast(String::compareTo))
                .thenComparing(PackTemplateField::name, Comparator.nullsLast(String::compareTo));
    }

    private List<String> normalizedBusinessTags(String tags) {
        if (isBlank(tags)) {
            return List.of();
        }
        return Arrays.stream(tags.split("[,，]"))
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .filter(tag -> !tag.startsWith(SOURCE_TAG_PREFIX))
                .distinct()
                .sorted()
                .toList();
    }

    private boolean hasPackSourceMarker(String tags, String packKey) {
        if (isBlank(tags) || isBlank(packKey)) {
            return false;
        }
        String expectedPrefix = SOURCE_TAG_PREFIX + packKey + "@";
        return Arrays.stream(tags.split("[,，]"))
                .map(String::trim)
                .anyMatch(tag -> tag.startsWith(expectedPrefix));
    }

    private Long requiredProjectId(Long projectId) {
        if (projectId == null) {
            throw new BizException("项目 ID 不能为空");
        }
        return projectId;
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new BizException(message);
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String textOrDefault(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private int count(Map<String, Long> counts, String key) {
        return counts.getOrDefault(key, 0L).intValue();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BizException("标准复用包 JSON 序列化失败: " + e.getMessage());
        }
    }

    private <T> T readValue(String json, Class<T> clazz, T fallback) {
        if (json == null || json.isBlank()) {
            return fallback;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            return fallback;
        }
    }

    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new BizException("标准复用包 hash 计算失败: " + e.getMessage());
        }
    }

    private <T> List<T> safeList(List<T> list) {
        return list == null ? List.of() : list;
    }

    private <T> Map<String, T> toMap(List<T> list, Function<T, String> keyExtractor) {
        return safeList(list).stream()
                .filter(item -> !isBlank(keyExtractor.apply(item)))
                .collect(toNaturalMap(keyExtractor));
    }

    private <T> java.util.stream.Collector<T, ?, Map<String, T>> toNaturalMap(Function<T, String> keyExtractor) {
        return Collectors.toMap(keyExtractor, Function.identity(), (a, b) -> a, LinkedHashMap::new);
    }

    private record ApplyContext(StandardReusePack pack, PackPayload payload, Long targetProjectId) {
    }

    private static final class ApplyAccumulator {
        private StandardReusePackAssetCounts created = StandardReusePackAssetCounts.empty();
        private StandardReusePackAssetCounts skipped = StandardReusePackAssetCounts.empty();
    }

    private static final class DriftAccumulator {
        private StandardReusePackDriftCounts counts = StandardReusePackDriftCounts.empty();
        private final List<StandardReusePackPlanItem> items = new ArrayList<>();

        private void add(StandardReusePackPlanItem item) {
            items.add(item);
            counts = switch (item.action()) {
                case "MATCHED" -> counts.plusMatched(1);
                case "MISSING" -> counts.plusMissing(1);
                case "OVERRIDDEN" -> counts.plusOverridden(1);
                case "DRIFTED" -> counts.plusDrifted(1);
                default -> counts;
            };
        }
    }

    private record TargetCatalog(
            Map<String, Domain> domainsByCode,
            Map<String, EnumDict> enumsByCode,
            Map<String, List<EnumValue>> enumValuesByEnumCode,
            Map<String, Field> fieldsByName,
            Map<Long, String> fieldNamesById,
            Map<String, RuleConfig> rulesByCode,
            Map<String, Template> templatesByName,
            Map<String, List<TemplateField>> templateFieldsByTemplateName
    ) {
        private Map<Long, String> domainCodesById() {
            return domainsByCode.values().stream()
                    .filter(domain -> domain.getId() != null)
                    .collect(Collectors.toMap(Domain::getId, Domain::getCode, (a, b) -> a));
        }

        private Map<Long, String> enumCodesById() {
            return enumsByCode.values().stream()
                    .filter(dict -> dict.getId() != null)
                    .collect(Collectors.toMap(EnumDict::getId, EnumDict::getCode, (a, b) -> a));
        }
    }

    private record PackPayload(
            int schemaVersion,
            Long sourceProjectId,
            String sourceProjectName,
            String packKey,
            String packName,
            String basePackVersion,
            String description,
            List<PackDomain> domains,
            List<PackEnum> enums,
            List<PackField> fields,
            List<PackRule> rules,
            List<PackTemplate> templates
    ) {
    }

    private record PackDomain(String code, String name, String description) {
    }

    private record PackEnum(String code, String name, String description, String valueType, List<PackEnumValue> values) {
    }

    private record PackEnumValue(String value, String label, Integer sortOrder) {
    }

    private record PackField(
            String name,
            String displayName,
            String dataType,
            Integer length,
            Integer precisionVal,
            Integer scaleVal,
            Boolean nullable,
            String defaultValue,
            String comment,
            String domainCode,
            String tags,
            String aliases,
            String category,
            String codeSetCode,
            Boolean sensitive,
            String status,
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
            String preferredUseCases,
            String avoidWhen,
            String joinHints,
            String defaultFilters,
            String aggregationHints,
            String replacementGuidance,
            String misuseExamples
    ) {
    }

    private record PackRule(String ruleCode, String ruleName, String severity, Boolean enabled, String paramsJson) {
    }

    private record PackTemplate(String name, String description, String tablePrefix, List<PackTemplateField> fields) {
    }

    private record PackTemplateField(
            String fieldName,
            String name,
            String dataType,
            Boolean nullable,
            String defaultValue,
            String comment,
            Integer sortOrder,
            Boolean isRequired
    ) {
        private String naturalName() {
            return fieldName == null ? name : fieldName;
        }
    }
}
