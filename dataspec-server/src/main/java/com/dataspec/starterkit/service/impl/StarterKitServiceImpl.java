package com.dataspec.starterkit.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.domain.entity.Domain;
import com.dataspec.domain.repository.DomainRepository;
import com.dataspec.enumdict.entity.EnumDict;
import com.dataspec.enumdict.entity.EnumValue;
import com.dataspec.enumdict.repository.EnumDictRepository;
import com.dataspec.field.entity.Field;
import com.dataspec.field.repository.FieldRepository;
import com.dataspec.security.context.DataSpecSecurityContext;
import com.dataspec.security.context.ProjectAccessGuard;
import com.dataspec.starterkit.entity.StarterKitInstallation;
import com.dataspec.starterkit.model.StarterKitApplyCounts;
import com.dataspec.starterkit.model.StarterKitApplyResult;
import com.dataspec.starterkit.model.StarterKitDefinition;
import com.dataspec.starterkit.model.StarterKitDomain;
import com.dataspec.starterkit.model.StarterKitEnumDefinition;
import com.dataspec.starterkit.model.StarterKitEnumValue;
import com.dataspec.starterkit.model.StarterKitFieldDefinition;
import com.dataspec.starterkit.model.StarterKitInstallationInfo;
import com.dataspec.starterkit.model.StarterKitTemplateDefinition;
import com.dataspec.starterkit.model.StarterKitTemplateField;
import com.dataspec.starterkit.repository.StarterKitInstallationRepository;
import com.dataspec.starterkit.service.BuiltInDomainStarterKits;
import com.dataspec.starterkit.service.StarterKitService;
import com.dataspec.template.entity.Template;
import com.dataspec.template.entity.TemplateField;
import com.dataspec.template.repository.TemplateRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StarterKitServiceImpl implements StarterKitService {

    public static final String SOURCE_TAG_PREFIX = "starter:";

    private static final String DEFAULT_STATUS = "enabled";

    private final DomainRepository domainRepository;
    private final EnumDictRepository enumDictRepository;
    private final FieldRepository fieldRepository;
    private final TemplateRepository templateRepository;
    private final StarterKitInstallationRepository installationRepository;
    private final ObjectMapper objectMapper;

    @Override
    public List<StarterKitDefinition> listKits() {
        return BuiltInDomainStarterKits.list();
    }

    @Override
    @Transactional
    public StarterKitApplyResult applyKit(Long projectId, String kitKey, String kitVersion) {
        ProjectAccessGuard.requireProjectAccess(projectId);
        StarterKitDefinition kit = resolveKit(kitKey, kitVersion);
        ApplyAccumulator acc = new ApplyAccumulator();
        String sourceTag = sourceTag(kit);

        Map<String, Domain> domainsByCode = loadDomains(projectId);
        Map<String, EnumDict> enumsByCode = loadEnums(projectId);
        Map<String, Field> fieldsByName = loadFields(projectId);

        applyDomains(projectId, kit, domainsByCode, acc);
        applyEnums(projectId, kit, enumsByCode, acc);
        applyFields(projectId, kit, sourceTag, domainsByCode, enumsByCode, fieldsByName, acc);
        applyTemplates(projectId, kit, fieldsByName, acc);

        LocalDateTime appliedAt = LocalDateTime.now();
        StarterKitApplyResult result = acc.toResult(projectId, kit, appliedAt);
        saveInstallation(projectId, kit, result);
        return result;
    }

    @Override
    public List<StarterKitInstallationInfo> listInstallations(Long projectId) {
        ProjectAccessGuard.requireProjectAccess(projectId);
        return installationRepository.findByProjectId(projectId).stream()
                .map(this::toInfo)
                .toList();
    }

    private StarterKitDefinition resolveKit(String kitKey, String kitVersion) {
        StarterKitDefinition kit = BuiltInDomainStarterKits.find(kitKey)
                .orElseThrow(() -> new BizException("Starter Kit 不存在: " + kitKey));
        if (kitVersion != null && !kitVersion.isBlank() && !kit.version().equals(kitVersion.trim())) {
            throw new BizException("Starter Kit 版本不匹配: " + kitKey + "@" + kitVersion);
        }
        return kit;
    }

    private void applyDomains(
            Long projectId,
            StarterKitDefinition kit,
            Map<String, Domain> domainsByCode,
            ApplyAccumulator acc
    ) {
        for (StarterKitDomain seed : safeList(kit.domains())) {
            if (isBlank(seed.code())) {
                acc.warn("Starter Kit " + kit.key() + " 包含空数据域编码,已跳过");
                continue;
            }
            Domain existing = domainsByCode.get(seed.code());
            if (existing != null) {
                acc.skipped = acc.skipped.plusDomains(1);
                continue;
            }
            Domain domain = new Domain();
            domain.setProjectId(projectId);
            domain.setCode(seed.code());
            domain.setName(seed.name());
            domain.setDescription(seed.description());
            domainRepository.insert(domain);
            domainsByCode.put(seed.code(), domain);
            acc.created = acc.created.plusDomains(1);
        }
    }

    private void applyEnums(
            Long projectId,
            StarterKitDefinition kit,
            Map<String, EnumDict> enumsByCode,
            ApplyAccumulator acc
    ) {
        for (StarterKitEnumDefinition seed : safeList(kit.enums())) {
            if (isBlank(seed.code())) {
                acc.warn("Starter Kit " + kit.key() + " 包含空枚举编码,已跳过");
                continue;
            }
            EnumDict dict = enumsByCode.get(seed.code());
            if (dict == null) {
                dict = new EnumDict();
                dict.setProjectId(projectId);
                dict.setCode(seed.code());
                dict.setName(seed.name());
                dict.setDescription(seed.description());
                dict.setValueType(textOrDefault(seed.valueType(), "string"));
                enumDictRepository.insertDict(dict);
                enumsByCode.put(seed.code(), dict);
                acc.created = acc.created.plusEnums(1);
                acc.createdEnums.add(seed.code());
            } else {
                acc.skipped = acc.skipped.plusEnums(1);
                acc.skippedEnums.add(seed.code());
            }
            applyEnumValues(dict, seed, acc);
        }
    }

    private void applyEnumValues(EnumDict dict, StarterKitEnumDefinition seed, ApplyAccumulator acc) {
        for (StarterKitEnumValue valueSeed : safeList(seed.values())) {
            if (isBlank(valueSeed.value())) {
                acc.warn("枚举 " + seed.code() + " 包含空枚举值,已跳过");
                continue;
            }
            if (enumDictRepository.existsValueByEnumIdAndValue(dict.getId(), valueSeed.value())) {
                acc.skipped = acc.skipped.plusEnumValues(1);
                continue;
            }
            EnumValue value = new EnumValue();
            value.setEnumId(dict.getId());
            value.setValue(valueSeed.value());
            value.setLabel(valueSeed.label());
            value.setSortOrder(valueSeed.sortOrder());
            enumDictRepository.insertValue(value);
            acc.created = acc.created.plusEnumValues(1);
        }
    }

    private void applyFields(
            Long projectId,
            StarterKitDefinition kit,
            String sourceTag,
            Map<String, Domain> domainsByCode,
            Map<String, EnumDict> enumsByCode,
            Map<String, Field> fieldsByName,
            ApplyAccumulator acc
    ) {
        for (StarterKitFieldDefinition seed : safeList(kit.fields())) {
            if (isBlank(seed.name())) {
                acc.warn("Starter Kit " + kit.key() + " 包含空字段名,已跳过");
                continue;
            }
            if (fieldsByName.containsKey(seed.name())) {
                acc.skipped = acc.skipped.plusFields(1);
                acc.skippedFields.add(seed.name());
                continue;
            }
            Field field = toField(projectId, seed, sourceTag, domainsByCode, enumsByCode, acc);
            fieldRepository.insert(field);
            fieldsByName.put(field.getName(), field);
            acc.created = acc.created.plusFields(1);
            acc.createdFields.add(field.getName());
        }
    }

    private Field toField(
            Long projectId,
            StarterKitFieldDefinition seed,
            String sourceTag,
            Map<String, Domain> domainsByCode,
            Map<String, EnumDict> enumsByCode,
            ApplyAccumulator acc
    ) {
        Field field = new Field();
        field.setProjectId(projectId);
        field.setName(seed.name());
        field.setDisplayName(seed.displayName());
        field.setDataType(seed.dataType());
        field.setLength(seed.length());
        field.setPrecisionVal(seed.precisionVal());
        field.setScaleVal(seed.scaleVal());
        field.setNullable(seed.nullable() != null ? seed.nullable() : true);
        field.setDefaultValue(seed.defaultValue());
        field.setComment(seed.comment());
        field.setDomainId(resolveDomainId(seed, domainsByCode, acc));
        field.setTags(mergeTags(seed.tags(), sourceTag));
        field.setAliases(seed.aliases());
        field.setCategory(seed.category());
        field.setCodeSetId(resolveCodeSetId(seed, enumsByCode, acc));
        field.setSensitive(Boolean.TRUE.equals(seed.sensitive()));
        field.setStatus(textOrDefault(seed.status(), DEFAULT_STATUS));
        field.setExampleValue(seed.exampleValue());
        return field;
    }

    private Long resolveDomainId(
            StarterKitFieldDefinition seed,
            Map<String, Domain> domainsByCode,
            ApplyAccumulator acc
    ) {
        if (isBlank(seed.domainCode())) {
            return null;
        }
        Domain domain = domainsByCode.get(seed.domainCode());
        if (domain == null) {
            acc.warn("字段 " + seed.name() + " 指向未知数据域 " + seed.domainCode());
            return null;
        }
        return domain.getId();
    }

    private Long resolveCodeSetId(
            StarterKitFieldDefinition seed,
            Map<String, EnumDict> enumsByCode,
            ApplyAccumulator acc
    ) {
        if (isBlank(seed.codeSetCode())) {
            return null;
        }
        EnumDict dict = enumsByCode.get(seed.codeSetCode());
        if (dict == null) {
            acc.warn("字段 " + seed.name() + " 指向未知枚举 " + seed.codeSetCode());
            return null;
        }
        return dict.getId();
    }

    private void applyTemplates(
            Long projectId,
            StarterKitDefinition kit,
            Map<String, Field> fieldsByName,
            ApplyAccumulator acc
    ) {
        for (StarterKitTemplateDefinition seed : safeList(kit.templates())) {
            if (isBlank(seed.name())) {
                acc.warn("Starter Kit " + kit.key() + " 包含空模板名,已跳过");
                continue;
            }
            if (templateRepository.findByNameInProject(seed.name(), projectId).isPresent()) {
                acc.skipped = acc.skipped.plusTemplates(1)
                        .plusTemplateFields(safeList(seed.fields()).size());
                acc.skippedTemplates.add(seed.name());
                continue;
            }
            Template template = new Template();
            template.setProjectId(projectId);
            template.setName(seed.name());
            template.setDescription(seed.description());
            template.setTablePrefix(seed.tablePrefix());
            templateRepository.insert(template);
            acc.created = acc.created.plusTemplates(1);
            acc.createdTemplates.add(seed.name());

            for (StarterKitTemplateField fieldSeed : safeList(seed.fields())) {
                TemplateField templateField = toTemplateField(template.getId(), fieldSeed, fieldsByName, acc);
                if (templateField == null) {
                    continue;
                }
                templateRepository.insertField(templateField);
                acc.created = acc.created.plusTemplateFields(1);
            }
        }
    }

    private TemplateField toTemplateField(
            Long templateId,
            StarterKitTemplateField seed,
            Map<String, Field> fieldsByName,
            ApplyAccumulator acc
    ) {
        Field linked = isBlank(seed.fieldName()) ? null : fieldsByName.get(seed.fieldName());
        if (!isBlank(seed.fieldName()) && linked == null) {
            acc.warn("模板字段引用未知标准字段 " + seed.fieldName() + ",已跳过");
            return null;
        }
        TemplateField field = new TemplateField();
        field.setTemplateId(templateId);
        field.setFieldId(linked == null ? null : linked.getId());
        field.setName(linked == null ? seed.name() : linked.getName());
        field.setDataType(textOrDefault(seed.dataType(), linked == null ? null : linked.getDataType()));
        field.setNullable(seed.nullable() != null ? seed.nullable() : linked == null ? true : linked.getNullable());
        field.setDefaultValue(seed.defaultValue() != null ? seed.defaultValue() : linked == null ? null : linked.getDefaultValue());
        field.setComment(textOrDefault(seed.comment(), linked == null ? null : linked.getComment()));
        field.setSortOrder(seed.sortOrder());
        field.setIsRequired(Boolean.TRUE.equals(seed.required()));
        return field;
    }

    private void saveInstallation(Long projectId, StarterKitDefinition kit, StarterKitApplyResult result) {
        StarterKitInstallation installation = new StarterKitInstallation();
        installation.setProjectId(projectId);
        installation.setKitKey(kit.key());
        installation.setKitName(kit.name());
        installation.setKitVersion(kit.version());
        installation.setCreatedCountsJson(writeJson(result.created()));
        installation.setSkippedCountsJson(writeJson(result.skipped()));
        installation.setWarningsJson(writeJson(result.warnings()));
        installation.setOperatorName(DataSpecSecurityContext.currentOperator());
        installation.setAppliedAt(result.appliedAt());
        installationRepository.insert(installation);
    }

    private StarterKitInstallationInfo toInfo(StarterKitInstallation entity) {
        return new StarterKitInstallationInfo(
                entity.getId(),
                entity.getProjectId(),
                entity.getKitKey(),
                entity.getKitName(),
                entity.getKitVersion(),
                readCounts(entity.getCreatedCountsJson()),
                readCounts(entity.getSkippedCountsJson()),
                readWarnings(entity.getWarningsJson()),
                entity.getOperatorName(),
                entity.getAppliedAt());
    }

    private Map<String, Domain> loadDomains(Long projectId) {
        Map<String, Domain> map = new LinkedHashMap<>();
        for (Domain domain : domainRepository.findByProjectId(projectId)) {
            map.put(domain.getCode(), domain);
        }
        return map;
    }

    private Map<String, EnumDict> loadEnums(Long projectId) {
        Map<String, EnumDict> map = new LinkedHashMap<>();
        for (EnumDict dict : enumDictRepository.findDictsByProjectId(projectId)) {
            map.put(dict.getCode(), dict);
        }
        return map;
    }

    private Map<String, Field> loadFields(Long projectId) {
        Map<String, Field> map = new LinkedHashMap<>();
        for (Field field : fieldRepository.findAllByProjectId(projectId)) {
            map.put(field.getName(), field);
        }
        return map;
    }

    private String sourceTag(StarterKitDefinition kit) {
        return SOURCE_TAG_PREFIX + kit.key() + "@" + kit.version();
    }

    private String mergeTags(String tags, String sourceTag) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (tags != null) {
            for (String tag : tags.split("[,，]")) {
                if (!tag.isBlank()) {
                    merged.add(tag.trim());
                }
            }
        }
        merged.add(sourceTag);
        return String.join(",", merged);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BizException("Starter Kit 应用结果序列化失败: " + e.getMessage());
        }
    }

    private StarterKitApplyCounts readCounts(String json) {
        if (isBlank(json)) {
            return StarterKitApplyCounts.empty();
        }
        try {
            return objectMapper.readValue(json, StarterKitApplyCounts.class);
        } catch (Exception e) {
            return StarterKitApplyCounts.empty();
        }
    }

    private List<String> readWarnings(String json) {
        if (isBlank(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private String textOrDefault(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static final class ApplyAccumulator {
        private StarterKitApplyCounts created = StarterKitApplyCounts.empty();
        private StarterKitApplyCounts skipped = StarterKitApplyCounts.empty();
        private final List<String> createdFields = new ArrayList<>();
        private final List<String> skippedFields = new ArrayList<>();
        private final List<String> createdEnums = new ArrayList<>();
        private final List<String> skippedEnums = new ArrayList<>();
        private final List<String> createdTemplates = new ArrayList<>();
        private final List<String> skippedTemplates = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        private void warn(String warning) {
            warnings.add(warning);
        }

        private StarterKitApplyResult toResult(Long projectId, StarterKitDefinition kit, LocalDateTime appliedAt) {
            return new StarterKitApplyResult(
                    projectId,
                    kit.key(),
                    kit.name(),
                    kit.version(),
                    created,
                    skipped,
                    List.copyOf(createdFields),
                    List.copyOf(skippedFields),
                    List.copyOf(createdEnums),
                    List.copyOf(skippedEnums),
                    List.copyOf(createdTemplates),
                    List.copyOf(skippedTemplates),
                    List.copyOf(warnings),
                    appliedAt);
        }
    }
}
