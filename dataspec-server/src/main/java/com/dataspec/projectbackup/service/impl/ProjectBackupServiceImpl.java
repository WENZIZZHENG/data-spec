package com.dataspec.projectbackup.service.impl;

import com.dataspec.changelog.entity.StandardChangeLog;
import com.dataspec.changelog.repository.StandardChangeLogRepository;
import com.dataspec.common.exception.BizException;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.domain.entity.Domain;
import com.dataspec.domain.repository.DomainRepository;
import com.dataspec.enumdict.entity.EnumDict;
import com.dataspec.enumdict.entity.EnumValue;
import com.dataspec.enumdict.repository.EnumDictRepository;
import com.dataspec.field.entity.Field;
import com.dataspec.field.repository.FieldRepository;
import com.dataspec.idempotency.WriteGuardService;
import com.dataspec.project.entity.Project;
import com.dataspec.project.repository.ProjectRepository;
import com.dataspec.project.service.ProjectService;
import com.dataspec.projectbackup.entity.ProjectRestoreRecord;
import com.dataspec.projectbackup.model.BackupProject;
import com.dataspec.projectbackup.model.EnumDictBackup;
import com.dataspec.projectbackup.model.ProjectBackupAssets;
import com.dataspec.projectbackup.model.ProjectBackupCounts;
import com.dataspec.projectbackup.model.ProjectBackupPackage;
import com.dataspec.projectbackup.model.ProjectBackupSanitization;
import com.dataspec.projectbackup.model.ProjectRestoreCounts;
import com.dataspec.projectbackup.model.ProjectRestoreItem;
import com.dataspec.projectbackup.model.ProjectRestorePlan;
import com.dataspec.projectbackup.model.ProjectRestoreReq;
import com.dataspec.projectbackup.model.ProjectRestoreResult;
import com.dataspec.projectbackup.model.TemplateBackup;
import com.dataspec.projectbackup.repository.ProjectRestoreRecordRepository;
import com.dataspec.projectbackup.service.ProjectBackupService;
import com.dataspec.reverseimport.entity.FieldSource;
import com.dataspec.reverseimport.entity.ReverseImportBatch;
import com.dataspec.reverseimport.repository.FieldSourceRepository;
import com.dataspec.reverseimport.repository.ReverseImportBatchRepository;
import com.dataspec.rule.entity.RuleConfig;
import com.dataspec.rule.repository.RuleConfigRepository;
import com.dataspec.rule.service.RuleConfigService;
import com.dataspec.rulebaseline.model.RuleBaselinePackage;
import com.dataspec.rulebaseline.service.RuleBaselineService;
import com.dataspec.security.context.DataSpecSecurityContext;
import com.dataspec.security.context.ProjectAccessGuard;
import com.dataspec.standard.entity.StandardSnapshot;
import com.dataspec.standard.repository.StandardSnapshotRepository;
import com.dataspec.template.entity.Template;
import com.dataspec.template.entity.TemplateField;
import com.dataspec.template.repository.TemplateRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectBackupServiceImpl implements ProjectBackupService {

    private static final int SCHEMA_VERSION = 1;
    private static final int CHANGE_LOG_EXPORT_LIMIT = 200;
    private final ProjectRepository projectRepository;
    private final ProjectService projectService;
    private final DomainRepository domainRepository;
    private final FieldRepository fieldRepository;
    private final EnumDictRepository enumDictRepository;
    private final RuleConfigRepository ruleConfigRepository;
    private final RuleConfigService ruleConfigService;
    private final RuleBaselineService ruleBaselineService;
    private final TemplateRepository templateRepository;
    private final StandardSnapshotRepository standardSnapshotRepository;
    private final ReverseImportBatchRepository reverseImportBatchRepository;
    private final FieldSourceRepository fieldSourceRepository;
    private final StandardChangeLogRepository changeLogRepository;
    private final ProjectRestoreRecordRepository restoreRecordRepository;
    private final ObjectMapper objectMapper;
    private WriteGuardService writeGuardService = new WriteGuardService();

    @Override
    public ProjectBackupPackage exportPackage(Long projectId) {
        Project project = projectService.getById(projectId);
        ProjectBackupAssets assets = exportAssets(projectId);
        ProjectBackupPackage withoutHash = new ProjectBackupPackage(
                SCHEMA_VERSION,
                LocalDateTime.now(),
                toBackupProject(project),
                assets,
                countAssets(assets),
                sanitization(),
                List.of("备份包不包含 API token、数据库密码、完整连接串或源数据库业务数据行"),
                null);
        String packageHash = packageHash(withoutHash);
        return withHash(withoutHash, packageHash);
    }

    @Override
    public ProjectRestorePlan previewRestore(ProjectRestoreReq req) {
        ProjectBackupPackage pkg = validatePackage(req.backupPackage());
        boolean overwrite = Boolean.TRUE.equals(req.overwrite());
        Project target = targetProjectForPreview(req.targetProjectId());
        String targetName = target == null ? uniqueProjectName(pkg.sourceProject().name()) : target.getName();
        List<ProjectRestoreItem> items = buildPlanItems(pkg, target, overwrite);
        ProjectRestoreCounts counts = countPlan(items);
        List<String> warnings = new ArrayList<>(safeList(pkg.warnings()));
        if (target == null) {
            warnings.add("未指定 targetProjectId，应用恢复时将创建新项目");
        }
        boolean canApply = counts.blocked() == 0 && counts.conflicts() == 0;
        return new ProjectRestorePlan(
                true,
                overwrite,
                canApply,
                "SUPPORTED",
                target == null ? null : target.getId(),
                targetName,
                counts,
                items,
                warnings);
    }

    @Override
    @Transactional
    public ProjectRestoreResult applyRestore(ProjectRestoreReq req) {
        return applyRestore(req, null);
    }

    @Override
    @Transactional
    public ProjectRestoreResult applyRestore(ProjectRestoreReq req, String idempotencyKey) {
        Long lockProjectId = restoreLockProjectId(req);
        if (req.targetProjectId() == null) {
            ProjectAccessGuard.requireAllProjects("恢复到新项目需要全项目 API token");
        } else {
            ProjectAccessGuard.requireProjectAccess(lockProjectId);
        }
        return writeGuardService.execute(lockProjectId, "project-backup:restore-apply", idempotencyKey,
                () -> applyRestoreInternal(req));
    }

    private ProjectRestoreResult applyRestoreInternal(ProjectRestoreReq req) {
        ProjectRestorePlan preview = previewRestore(req);
        if (!Boolean.TRUE.equals(preview.canApply())) {
            throw new BizException("恢复计划存在冲突或阻断项，请先处理 dry-run 结果");
        }
        Project target = resolveTargetProject(req.backupPackage(), req.targetProjectId(), preview.targetProjectName());
        RestoreContext context = new RestoreContext(target.getId(), Boolean.TRUE.equals(req.overwrite()));
        restoreDomains(req.backupPackage(), context);
        restoreEnumDicts(req.backupPackage(), context);
        restoreFields(req.backupPackage(), context);
        restoreRules(req.backupPackage(), context);
        restoreRuleBaseline(req.backupPackage(), target.getId(), context.overwrite);
        restoreTemplates(req.backupPackage(), context);
        restoreSnapshots(req.backupPackage(), context);
        restoreReverseImportSources(req.backupPackage(), context);

        ProjectRestorePlan appliedPlan = new ProjectRestorePlan(
                false,
                preview.overwrite(),
                true,
                preview.compatibilityStatus(),
                target.getId(),
                target.getName(),
                preview.counts(),
                preview.items(),
                preview.warnings());
        ProjectRestoreRecord record = saveRestoreRecord(req.backupPackage(), appliedPlan, target.getId());
        return new ProjectRestoreResult(appliedPlan, record);
    }

    @Autowired
    void setWriteGuardService(WriteGuardService writeGuardService) {
        this.writeGuardService = writeGuardService;
    }

    @Override
    public List<ProjectRestoreRecord> listRestoreRecords(Long projectId) {
        ProjectAccessGuard.requireProjectAccess(projectId);
        return restoreRecordRepository.findByProjectId(projectId);
    }

    private ProjectBackupAssets exportAssets(Long projectId) {
        List<Domain> domains = domainRepository.findByProjectId(projectId).stream()
                .map(this::copyForBackup)
                .toList();
        List<Field> fields = fieldRepository.findAllByProjectId(projectId).stream()
                .map(this::copyForBackup)
                .toList();
        List<EnumDictBackup> enumDicts = enumDictRepository.findDictsByProjectId(projectId).stream()
                .map(dict -> new EnumDictBackup(
                        copyForBackup(dict),
                        enumDictRepository.findValuesByEnumId(dict.getId()).stream()
                                .map(this::copyForBackup)
                                .toList()))
                .toList();
        List<RuleConfig> rules = ruleConfigRepository.findByProjectId(projectId).stream()
                .map(this::copyForBackup)
                .toList();
        RuleBaselinePackage ruleBaseline = ruleBaselineService.exportBaseline(projectId);
        List<TemplateBackup> templates = templateRepository.findByProjectId(projectId).stream()
                .map(template -> new TemplateBackup(
                        copyForBackup(template),
                        templateRepository.findFieldsByTemplateId(template.getId()).stream()
                                .map(this::copyForBackup)
                                .toList()))
                .toList();
        List<StandardSnapshot> snapshots = standardSnapshotRepository.findByProjectId(projectId).stream()
                .map(this::copyForBackup)
                .toList();
        List<ReverseImportBatch> batches = reverseImportBatchRepository.findByProjectId(projectId).stream()
                .map(this::copyForBackup)
                .toList();
        List<FieldSource> sources = fieldSourceRepository.findByProjectId(projectId).stream()
                .map(this::copySourceForBackup)
                .toList();
        List<StandardChangeLog> logs = changeLogRepository.findByProjectId(projectId, CHANGE_LOG_EXPORT_LIMIT).stream()
                .map(this::copyForBackup)
                .toList();
        return new ProjectBackupAssets(domains, fields, enumDicts, rules, ruleBaseline, templates, snapshots, batches, sources, logs);
    }

    private ProjectBackupPackage validatePackage(ProjectBackupPackage pkg) {
        if (pkg == null) {
            throw new BizException("备份包不能为空");
        }
        if (!Objects.equals(pkg.schemaVersion(), SCHEMA_VERSION)) {
            throw new BizException("不支持的备份包 schemaVersion: " + pkg.schemaVersion());
        }
        if (pkg.sourceProject() == null || pkg.assets() == null) {
            throw new BizException("备份包缺少 sourceProject 或 assets");
        }
        String expectedHash = packageHash(withHash(pkg, null));
        if (pkg.packageHash() == null || !pkg.packageHash().equals(expectedHash)) {
            throw new BizException("备份包 packageHash 校验失败");
        }
        if (containsSensitivePayload(pkg)) {
            throw new BizException("备份包包含疑似敏感字段，请重新导出脱敏包");
        }
        return pkg;
    }

    private Long restoreLockProjectId(ProjectRestoreReq req) {
        if (req == null) {
            throw new BizException("恢复请求不能为空");
        }
        if (req.targetProjectId() != null) {
            return req.targetProjectId();
        }
        ProjectBackupPackage pkg = validatePackage(req.backupPackage());
        Long sourceProjectId = pkg.sourceProject() == null ? null : pkg.sourceProject().id();
        return sourceProjectId == null ? 0L : sourceProjectId;
    }

    private boolean containsSensitivePayload(ProjectBackupPackage pkg) {
        try {
            JsonNode root = mapper().valueToTree(pkg);
            return SensitiveDataSanitizer.containsSensitiveKeyOrValue(root);
        } catch (Exception e) {
            throw new BizException("备份包安全扫描失败: " + e.getMessage());
        }
    }

    private Project targetProjectForPreview(Long targetProjectId) {
        if (targetProjectId == null) {
            return null;
        }
        return projectService.getById(targetProjectId);
    }

    private Project resolveTargetProject(ProjectBackupPackage pkg, Long targetProjectId, String plannedName) {
        if (targetProjectId != null) {
            return projectService.getById(targetProjectId);
        }
        ProjectAccessGuard.requireAllProjects("恢复到新项目需要全项目 API token");
        Project project = new Project();
        project.setName(plannedName);
        project.setDescription(pkg.sourceProject().description());
        project.setDbType(textOrDefault(pkg.sourceProject().dbType(), "postgresql"));
        return projectService.create(project, false);
    }

    private List<ProjectRestoreItem> buildPlanItems(ProjectBackupPackage pkg, Project target, boolean overwrite) {
        List<ProjectRestoreItem> items = new ArrayList<>();
        ProjectBackupAssets assets = pkg.assets();
        if (target == null) {
            addAllCreateItems(items, assets);
            return items;
        }
        Long targetProjectId = target.getId();
        Set<String> domains = domainRepository.findByProjectId(targetProjectId).stream().map(Domain::getCode).collect(Collectors.toSet());
        Set<String> fields = fieldRepository.findAllByProjectId(targetProjectId).stream().map(Field::getName).collect(Collectors.toSet());
        Set<String> enumDicts = enumDictRepository.findDictsByProjectId(targetProjectId).stream().map(EnumDict::getCode).collect(Collectors.toSet());
        Set<String> rules = ruleConfigRepository.findByProjectId(targetProjectId).stream().map(RuleConfig::getRuleCode).collect(Collectors.toSet());
        Set<String> templates = templateRepository.findByProjectId(targetProjectId).stream().map(Template::getName).collect(Collectors.toSet());
        Set<String> snapshots = standardSnapshotRepository.findByProjectId(targetProjectId).stream().map(StandardSnapshot::getVersion).collect(Collectors.toSet());

        safeList(assets.domains()).forEach(item -> addNaturalKeyItem(items, "domain", item.getCode(), domains, overwrite));
        safeList(assets.enumDicts()).forEach(item -> addNaturalKeyItem(items, "enum_dict", item.dict().getCode(), enumDicts, overwrite));
        safeList(assets.fields()).forEach(item -> addNaturalKeyItem(items, "field", item.getName(), fields, overwrite));
        safeList(assets.rules()).forEach(item -> addNaturalKeyItem(items, "rule", item.getRuleCode(), rules, overwrite));
        safeList(assets.templates()).forEach(item -> addNaturalKeyItem(items, "template", item.template().getName(), templates, overwrite));
        safeList(assets.snapshots()).forEach(item -> addNaturalKeyItem(items, "snapshot", item.getVersion(), snapshots, false));
        safeList(assets.reverseImportBatches()).forEach(item -> items.add(new ProjectRestoreItem("reverse_import_batch", String.valueOf(item.getId()), "CREATE", "来源摘要将按新项目复制")));
        return items;
    }

    private void addAllCreateItems(List<ProjectRestoreItem> items, ProjectBackupAssets assets) {
        safeList(assets.domains()).forEach(item -> items.add(new ProjectRestoreItem("domain", item.getCode(), "CREATE", "新项目资产")));
        safeList(assets.enumDicts()).forEach(item -> items.add(new ProjectRestoreItem("enum_dict", item.dict().getCode(), "CREATE", "新项目资产")));
        safeList(assets.fields()).forEach(item -> items.add(new ProjectRestoreItem("field", item.getName(), "CREATE", "新项目资产")));
        safeList(assets.rules()).forEach(item -> items.add(new ProjectRestoreItem("rule", item.getRuleCode(), "CREATE", "新项目资产")));
        safeList(assets.templates()).forEach(item -> items.add(new ProjectRestoreItem("template", item.template().getName(), "CREATE", "新项目资产")));
        safeList(assets.snapshots()).forEach(item -> items.add(new ProjectRestoreItem("snapshot", item.getVersion(), "CREATE", "新项目资产")));
        safeList(assets.reverseImportBatches()).forEach(item -> items.add(new ProjectRestoreItem("reverse_import_batch", String.valueOf(item.getId()), "CREATE", "新项目来源摘要")));
    }

    private void addNaturalKeyItem(List<ProjectRestoreItem> items, String type, String key, Set<String> existing, boolean overwrite) {
        if (key == null || key.isBlank()) {
            items.add(new ProjectRestoreItem(type, "", "BLOCKED", "缺少自然键"));
            return;
        }
        if (!existing.contains(key)) {
            items.add(new ProjectRestoreItem(type, key, "CREATE", "目标项目不存在"));
        } else if (overwrite) {
            items.add(new ProjectRestoreItem(type, key, "UPDATE", "目标项目已存在且允许覆盖"));
        } else {
            items.add(new ProjectRestoreItem(type, key, "SKIP", "目标项目已存在，默认不覆盖"));
        }
    }

    private ProjectRestoreCounts countPlan(List<ProjectRestoreItem> items) {
        Map<String, Long> counts = items.stream().collect(Collectors.groupingBy(ProjectRestoreItem::action, Collectors.counting()));
        int warnings = (int) items.stream().filter(item -> "CONFLICT".equals(item.action()) || "BLOCKED".equals(item.action())).count();
        return new ProjectRestoreCounts(
                count(counts, "CREATE"),
                count(counts, "UPDATE"),
                count(counts, "SKIP"),
                count(counts, "CONFLICT"),
                count(counts, "BLOCKED"),
                warnings);
    }

    private int count(Map<String, Long> counts, String key) {
        return counts.getOrDefault(key, 0L).intValue();
    }

    private void restoreDomains(ProjectBackupPackage pkg, RestoreContext context) {
        Map<Long, Long> idMap = context.domainIdMap;
        Map<String, Domain> existing = domainRepository.findByProjectId(context.projectId).stream()
                .collect(toNaturalMap(Domain::getCode));
        for (Domain source : safeList(pkg.assets().domains())) {
            Domain target = existing.get(source.getCode());
            if (target == null) {
                Domain created = copyForRestore(source, context.projectId);
                domainRepository.insert(created);
                idMap.put(source.getId(), created.getId());
            } else {
                if (context.overwrite) {
                    target.setName(source.getName());
                    target.setDescription(source.getDescription());
                    domainRepository.update(target);
                }
                idMap.put(source.getId(), target.getId());
            }
        }
    }

    private void restoreEnumDicts(ProjectBackupPackage pkg, RestoreContext context) {
        Map<String, EnumDict> existing = enumDictRepository.findDictsByProjectId(context.projectId).stream()
                .collect(toNaturalMap(EnumDict::getCode));
        for (EnumDictBackup backup : safeList(pkg.assets().enumDicts())) {
            EnumDict source = backup.dict();
            EnumDict target = existing.get(source.getCode());
            if (target == null) {
                target = copyForRestore(source, context.projectId);
                enumDictRepository.insertDict(target);
            } else if (context.overwrite) {
                target.setName(source.getName());
                target.setDescription(source.getDescription());
                target.setValueType(source.getValueType());
                enumDictRepository.updateDict(target);
            }
            context.enumIdMap.put(source.getId(), target.getId());
            restoreEnumValues(backup.values(), target.getId(), context.overwrite);
        }
    }

    private void restoreEnumValues(List<EnumValue> values, Long targetEnumId, boolean overwrite) {
        Map<String, EnumValue> existing = enumDictRepository.findValuesByEnumId(targetEnumId).stream()
                .collect(toNaturalMap(EnumValue::getValue));
        for (EnumValue source : safeList(values)) {
            EnumValue target = existing.get(source.getValue());
            if (target == null) {
                enumDictRepository.insertValue(copyForRestore(source, targetEnumId));
            } else if (overwrite) {
                target.setLabel(source.getLabel());
                target.setSortOrder(source.getSortOrder());
                enumDictRepository.updateValue(target);
            }
        }
    }

    private void restoreFields(ProjectBackupPackage pkg, RestoreContext context) {
        Map<String, Field> existing = fieldRepository.findAllByProjectId(context.projectId).stream()
                .collect(toNaturalMap(Field::getName));
        for (Field source : safeList(pkg.assets().fields())) {
            Field target = existing.get(source.getName());
            if (target == null) {
                Field created = copyForRestore(source, context.projectId, context);
                fieldRepository.insert(created);
                context.fieldIdMap.put(source.getId(), created.getId());
            } else {
                if (context.overwrite) {
                    copyFieldValues(source, target, context);
                    fieldRepository.update(target);
                }
                context.fieldIdMap.put(source.getId(), target.getId());
            }
        }
    }

    private void restoreRules(ProjectBackupPackage pkg, RestoreContext context) {
        Map<String, RuleConfig> existing = ruleConfigRepository.findByProjectId(context.projectId).stream()
                .collect(toNaturalMap(RuleConfig::getRuleCode));
        for (RuleConfig source : safeList(pkg.assets().rules())) {
            RuleConfig current = existing.get(source.getRuleCode());
            RuleConfig copy = copyForRestore(source, context.projectId);
            if (current == null) {
                ruleConfigService.create(copy);
            } else if (context.overwrite) {
                ruleConfigService.update(current.getId(), copy);
            }
        }
    }

    private void restoreRuleBaseline(ProjectBackupPackage pkg, Long targetProjectId, boolean overwrite) {
        if (pkg.assets().ruleBaseline() != null && pkg.assets().ruleBaseline().rules() != null && !pkg.assets().ruleBaseline().rules().isEmpty()) {
            ruleBaselineService.importBaseline(targetProjectId, pkg.assets().ruleBaseline(), overwrite);
        }
    }

    private void restoreTemplates(ProjectBackupPackage pkg, RestoreContext context) {
        Map<String, Template> existing = templateRepository.findByProjectId(context.projectId).stream()
                .collect(toNaturalMap(Template::getName));
        for (TemplateBackup backup : safeList(pkg.assets().templates())) {
            Template source = backup.template();
            Template target = existing.get(source.getName());
            if (target == null) {
                target = copyForRestore(source, context.projectId);
                templateRepository.insert(target);
            } else if (context.overwrite) {
                target.setDescription(source.getDescription());
                target.setTablePrefix(source.getTablePrefix());
                templateRepository.update(target);
                templateRepository.deleteFieldsByTemplateId(target.getId());
            } else {
                continue;
            }
            for (TemplateField field : safeList(backup.fields())) {
                templateRepository.insertField(copyForRestore(field, target.getId(), context));
            }
        }
    }

    private void restoreSnapshots(ProjectBackupPackage pkg, RestoreContext context) {
        Set<String> existingVersions = standardSnapshotRepository.findByProjectId(context.projectId).stream()
                .map(StandardSnapshot::getVersion)
                .collect(Collectors.toCollection(HashSet::new));
        for (StandardSnapshot source : safeList(pkg.assets().snapshots())) {
            if (existingVersions.contains(source.getVersion())) {
                continue;
            }
            standardSnapshotRepository.save(copyForRestore(source, context.projectId));
        }
    }

    private void restoreReverseImportSources(ProjectBackupPackage pkg, RestoreContext context) {
        for (ReverseImportBatch source : safeList(pkg.assets().reverseImportBatches())) {
            ReverseImportBatch batch = copyForRestore(source, context.projectId);
            reverseImportBatchRepository.insert(batch);
            context.batchIdMap.put(source.getId(), batch.getId());
        }
        for (FieldSource source : safeList(pkg.assets().fieldSources())) {
            Long fieldId = context.fieldIdMap.get(source.getFieldId());
            Long batchId = context.batchIdMap.get(source.getBatchId());
            if (fieldId == null || batchId == null) {
                continue;
            }
            fieldSourceRepository.insert(copyForRestore(source, context.projectId, fieldId, batchId));
        }
    }

    private ProjectRestoreRecord saveRestoreRecord(ProjectBackupPackage pkg, ProjectRestorePlan plan, Long targetProjectId) {
        try {
            ProjectRestoreRecord record = new ProjectRestoreRecord();
            record.setProjectId(targetProjectId);
            record.setPackageHash(pkg.packageHash());
            record.setSourceProjectName(pkg.sourceProject().name());
            record.setSourceProjectId(pkg.sourceProject().id());
            record.setSchemaVersion(pkg.schemaVersion());
            record.setDryRun(false);
            record.setOverwrite(plan.overwrite());
            record.setCreatedCount(plan.counts().created());
            record.setUpdatedCount(plan.counts().updated());
            record.setSkippedCount(plan.counts().skipped());
            record.setConflictCount(plan.counts().conflicts());
            record.setBlockedCount(plan.counts().blocked());
            record.setWarningCount(plan.counts().warnings());
            record.setSummaryJson(mapper().writeValueAsString(plan));
            record.setOperatorName(DataSpecSecurityContext.currentOperator());
            record.setCreatedAt(LocalDateTime.now());
            restoreRecordRepository.insert(record);
            return record;
        } catch (Exception e) {
            throw new BizException("保存恢复摘要失败: " + e.getMessage());
        }
    }

    private ProjectBackupCounts countAssets(ProjectBackupAssets assets) {
        int enumValueCount = safeList(assets.enumDicts()).stream().mapToInt(item -> safeList(item.values()).size()).sum();
        int templateFieldCount = safeList(assets.templates()).stream().mapToInt(item -> safeList(item.fields()).size()).sum();
        return new ProjectBackupCounts(
                safeList(assets.domains()).size(),
                safeList(assets.fields()).size(),
                safeList(assets.enumDicts()).size(),
                enumValueCount,
                safeList(assets.rules()).size(),
                safeList(assets.templates()).size(),
                templateFieldCount,
                safeList(assets.snapshots()).size(),
                safeList(assets.reverseImportBatches()).size(),
                safeList(assets.fieldSources()).size(),
                safeList(assets.changeLogs()).size());
    }

    private ProjectBackupSanitization sanitization() {
        return new ProjectBackupSanitization(
                true,
                List.of("apiToken.plainToken", "apiToken.tokenHash", "database.password", "database.jdbcUrl", "sourceDatabase.rows"),
                List.of("反向导入来源只保留结构元数据，不包含源库数据行"));
    }

    private BackupProject toBackupProject(Project project) {
        return new BackupProject(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getDbType(),
                project.getCreatedAt(),
                project.getUpdatedAt());
    }

    private String uniqueProjectName(String baseName) {
        String base = textOrDefault(baseName, "恢复项目");
        if (!projectRepository.existsByName(base)) {
            return base;
        }
        String dated = base + " - restored " + LocalDateTime.now().toLocalDate();
        if (!projectRepository.existsByName(dated)) {
            return dated;
        }
        return dated + " " + System.currentTimeMillis();
    }

    private ProjectBackupPackage withHash(ProjectBackupPackage pkg, String hash) {
        return new ProjectBackupPackage(
                pkg.schemaVersion(),
                pkg.exportedAt(),
                pkg.sourceProject(),
                pkg.assets(),
                pkg.counts(),
                pkg.sanitization(),
                pkg.warnings(),
                hash);
    }

    private String packageHash(ProjectBackupPackage pkg) {
        try {
            byte[] json = mapper().writeValueAsBytes(withHash(pkg, null));
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(json);
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new BizException("计算备份包 hash 失败: " + e.getMessage());
        }
    }

    private ObjectMapper mapper() {
        return objectMapper.copy()
                .findAndRegisterModules()
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
    }

    private <T> List<T> safeList(List<T> list) {
        return list == null ? List.of() : list;
    }

    private String textOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private <T> java.util.stream.Collector<T, ?, Map<String, T>> toNaturalMap(Function<T, String> keyFn) {
        return Collectors.toMap(
                item -> textOrDefault(keyFn.apply(item), ""),
                Function.identity(),
                (left, right) -> left,
                LinkedHashMap::new);
    }

    private Domain copyForBackup(Domain source) {
        Domain copy = copy(source, Domain.class);
        copy.setIsDeleted(null);
        return copy;
    }

    private Field copyForBackup(Field source) {
        Field copy = copy(source, Field.class);
        copy.setIsDeleted(null);
        return copy;
    }

    private EnumDict copyForBackup(EnumDict source) {
        EnumDict copy = copy(source, EnumDict.class);
        copy.setIsDeleted(null);
        return copy;
    }

    private EnumValue copyForBackup(EnumValue source) {
        EnumValue copy = copy(source, EnumValue.class);
        copy.setIsDeleted(null);
        return copy;
    }

    private RuleConfig copyForBackup(RuleConfig source) {
        RuleConfig copy = copy(source, RuleConfig.class);
        copy.setIsDeleted(null);
        return copy;
    }

    private Template copyForBackup(Template source) {
        Template copy = copy(source, Template.class);
        copy.setIsDeleted(null);
        return copy;
    }

    private TemplateField copyForBackup(TemplateField source) {
        TemplateField copy = copy(source, TemplateField.class);
        copy.setIsDeleted(null);
        return copy;
    }

    private StandardSnapshot copyForBackup(StandardSnapshot source) {
        StandardSnapshot copy = copy(source, StandardSnapshot.class);
        copy.setIsDeleted(null);
        return copy;
    }

    private ReverseImportBatch copyForBackup(ReverseImportBatch source) {
        return copy(source, ReverseImportBatch.class);
    }

    private FieldSource copySourceForBackup(FieldSource source) {
        FieldSource copy = copy(source, FieldSource.class);
        if (containsSensitiveSnapshot(copy.getMetadataJson())) {
            copy.setMetadataJson("{\"sanitized\":true}");
        }
        return copy;
    }

    private StandardChangeLog copyForBackup(StandardChangeLog source) {
        StandardChangeLog copy = copy(source, StandardChangeLog.class);
        copy.setBeforeJson(sanitizeSnapshot(copy.getBeforeJson()));
        copy.setAfterJson(sanitizeSnapshot(copy.getAfterJson()));
        return copy;
    }

    private String sanitizeSnapshot(String value) {
        if (value == null) {
            return null;
        }
        if (containsSensitiveSnapshot(value)) {
            return "{\"sanitized\":true}";
        }
        return value;
    }

    private boolean containsSensitiveSnapshot(String value) {
        if (value == null) {
            return false;
        }
        try {
            return SensitiveDataSanitizer.containsSensitiveKeyOrValue(mapper().readTree(value));
        } catch (Exception ignored) {
            return SensitiveDataSanitizer.containsSensitiveText(value);
        }
    }

    private Domain copyForRestore(Domain source, Long projectId) {
        Domain copy = copy(source, Domain.class);
        copy.setId(null);
        copy.setProjectId(projectId);
        copy.setIsDeleted(null);
        return copy;
    }

    private EnumDict copyForRestore(EnumDict source, Long projectId) {
        EnumDict copy = copy(source, EnumDict.class);
        copy.setId(null);
        copy.setProjectId(projectId);
        copy.setIsDeleted(null);
        return copy;
    }

    private EnumValue copyForRestore(EnumValue source, Long enumId) {
        EnumValue copy = copy(source, EnumValue.class);
        copy.setId(null);
        copy.setEnumId(enumId);
        copy.setIsDeleted(null);
        return copy;
    }

    private Field copyForRestore(Field source, Long projectId, RestoreContext context) {
        Field copy = copy(source, Field.class);
        copy.setId(null);
        copy.setProjectId(projectId);
        copy.setDomainId(context.domainIdMap.get(source.getDomainId()));
        copy.setCodeSetId(context.enumIdMap.get(source.getCodeSetId()));
        copy.setIsDeleted(null);
        return copy;
    }

    private void copyFieldValues(Field source, Field target, RestoreContext context) {
        target.setDisplayName(source.getDisplayName());
        target.setDataType(source.getDataType());
        target.setLength(source.getLength());
        target.setPrecisionVal(source.getPrecisionVal());
        target.setScaleVal(source.getScaleVal());
        target.setNullable(source.getNullable());
        target.setDefaultValue(source.getDefaultValue());
        target.setComment(source.getComment());
        target.setDomainId(context.domainIdMap.get(source.getDomainId()));
        target.setTags(source.getTags());
        target.setAliases(source.getAliases());
        target.setCategory(source.getCategory());
        target.setCodeSetId(context.enumIdMap.get(source.getCodeSetId()));
        target.setSensitive(source.getSensitive());
        target.setStatus(source.getStatus());
        target.setExampleValue(source.getExampleValue());
        target.setFormatType(source.getFormatType());
        target.setFormatPattern(source.getFormatPattern());
        target.setFormatUnit(source.getFormatUnit());
        target.setFormatPrecision(source.getFormatPrecision());
        target.setFormatTimezone(source.getFormatTimezone());
        target.setFormatNullPolicy(source.getFormatNullPolicy());
        target.setValidExamplesJson(source.getValidExamplesJson());
        target.setInvalidExamplesJson(source.getInvalidExamplesJson());
        target.setFormatNotes(source.getFormatNotes());
    }

    private RuleConfig copyForRestore(RuleConfig source, Long projectId) {
        RuleConfig copy = copy(source, RuleConfig.class);
        copy.setId(null);
        copy.setProjectId(projectId);
        copy.setIsDeleted(null);
        return copy;
    }

    private Template copyForRestore(Template source, Long projectId) {
        Template copy = copy(source, Template.class);
        copy.setId(null);
        copy.setProjectId(projectId);
        copy.setIsDeleted(null);
        return copy;
    }

    private TemplateField copyForRestore(TemplateField source, Long templateId, RestoreContext context) {
        TemplateField copy = copy(source, TemplateField.class);
        copy.setId(null);
        copy.setTemplateId(templateId);
        copy.setFieldId(context.fieldIdMap.get(source.getFieldId()));
        copy.setIsDeleted(null);
        return copy;
    }

    private StandardSnapshot copyForRestore(StandardSnapshot source, Long projectId) {
        StandardSnapshot copy = copy(source, StandardSnapshot.class);
        copy.setId(null);
        copy.setProjectId(projectId);
        copy.setIsDeleted(null);
        return copy;
    }

    private ReverseImportBatch copyForRestore(ReverseImportBatch source, Long projectId) {
        ReverseImportBatch copy = copy(source, ReverseImportBatch.class);
        copy.setId(null);
        copy.setProjectId(projectId);
        copy.setOperatorName(DataSpecSecurityContext.currentOperator());
        copy.setCreatedAt(LocalDateTime.now());
        return copy;
    }

    private FieldSource copyForRestore(FieldSource source, Long projectId, Long fieldId, Long batchId) {
        FieldSource copy = copy(source, FieldSource.class);
        copy.setId(null);
        copy.setProjectId(projectId);
        copy.setFieldId(fieldId);
        copy.setBatchId(batchId);
        copy.setCreatedAt(LocalDateTime.now());
        return copy;
    }

    private <T> T copy(T source, Class<T> targetType) {
        try {
            T target = targetType.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target);
            return target;
        } catch (Exception e) {
            throw new BizException("复制备份资产失败: " + e.getMessage());
        }
    }

    private static final class RestoreContext {
        private final Long projectId;
        private final boolean overwrite;
        private final Map<Long, Long> domainIdMap = new HashMap<>();
        private final Map<Long, Long> enumIdMap = new HashMap<>();
        private final Map<Long, Long> fieldIdMap = new HashMap<>();
        private final Map<Long, Long> batchIdMap = new HashMap<>();

        private RestoreContext(Long projectId, boolean overwrite) {
            this.projectId = projectId;
            this.overwrite = overwrite;
        }
    }
}
