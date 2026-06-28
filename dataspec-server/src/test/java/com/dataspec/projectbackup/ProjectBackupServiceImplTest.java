package com.dataspec.projectbackup;

import com.dataspec.changelog.repository.StandardChangeLogRepository;
import com.dataspec.common.exception.BizException;
import com.dataspec.domain.entity.Domain;
import com.dataspec.domain.repository.DomainRepository;
import com.dataspec.enumdict.repository.EnumDictRepository;
import com.dataspec.field.entity.Field;
import com.dataspec.field.repository.FieldRepository;
import com.dataspec.project.entity.Project;
import com.dataspec.project.repository.ProjectRepository;
import com.dataspec.project.service.ProjectService;
import com.dataspec.projectbackup.model.ProjectBackupPackage;
import com.dataspec.projectbackup.model.ProjectRestorePlan;
import com.dataspec.projectbackup.model.ProjectRestoreReq;
import com.dataspec.projectbackup.repository.ProjectRestoreRecordRepository;
import com.dataspec.projectbackup.service.impl.ProjectBackupServiceImpl;
import com.dataspec.reverseimport.repository.FieldSourceRepository;
import com.dataspec.reverseimport.repository.ReverseImportBatchRepository;
import com.dataspec.rule.repository.RuleConfigRepository;
import com.dataspec.rule.service.RuleConfigService;
import com.dataspec.rulebaseline.model.RuleBaselineInfo;
import com.dataspec.rulebaseline.model.RuleBaselinePackage;
import com.dataspec.rulebaseline.service.RuleBaselineService;
import com.dataspec.standard.repository.StandardSnapshotRepository;
import com.dataspec.template.repository.TemplateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectBackupServiceImplTest {

    @Test
    void exportPackage_sanitizesChangeLogAndBuildsHash() throws Exception {
        Fixture fixture = new Fixture();
        fixture.withSourceProject();
        fixture.withEmptyAssets();
        when(fixture.changeLogRepository.findByProjectId(1L, 200))
                .thenReturn(List.of(changeLogWithSecret()));
        ProjectBackupServiceImpl service = fixture.service();

        ProjectBackupPackage pkg = service.exportPackage(1L);
        String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(pkg);

        assertNotNull(pkg.packageHash());
        assertEquals(1, pkg.schemaVersion());
        assertEquals(1, pkg.counts().changeLogs());
        assertFalse(json.contains("super-secret"));
        assertFalse(json.contains("jdbc:postgresql://localhost/db"));
    }

    @Test
    void previewRestore_rejectsTamperedPackageHash() {
        Fixture fixture = new Fixture();
        fixture.withSourceProject();
        fixture.withEmptyAssets();
        ProjectBackupPackage exported = fixture.service().exportPackage(1L);
        ProjectBackupPackage tampered = new ProjectBackupPackage(
                exported.schemaVersion(),
                exported.exportedAt(),
                exported.sourceProject(),
                exported.assets(),
                exported.counts(),
                exported.sanitization(),
                exported.warnings(),
                "bad-hash");

        assertThrows(BizException.class,
                () -> fixture.service().previewRestore(new ProjectRestoreReq(null, false, tampered)));
    }

    @Test
    void previewRestore_marksExistingAssetsAsSkipAndDoesNotWrite() {
        Fixture fixture = new Fixture();
        fixture.withSourceProject();
        Domain sourceDomain = domain("order", 1L);
        Field sourceField = field("order_no", 1L);
        when(fixture.domainRepository.findByProjectId(1L)).thenReturn(List.of(sourceDomain));
        when(fixture.fieldRepository.findAllByProjectId(1L)).thenReturn(List.of(sourceField));
        fixture.withEmptyRemainingAssets();
        ProjectBackupPackage exported = fixture.service().exportPackage(1L);
        Project target = project(2L, "目标项目");
        when(fixture.projectService.getById(2L)).thenReturn(target);
        when(fixture.domainRepository.findByProjectId(2L)).thenReturn(List.of(domain("order", 2L)));
        when(fixture.fieldRepository.findAllByProjectId(2L)).thenReturn(List.of(field("order_no", 2L)));

        ProjectRestorePlan plan = fixture.service().previewRestore(new ProjectRestoreReq(2L, false, exported));

        assertEquals(2, plan.counts().skipped());
        assertEquals(0, plan.counts().created());
        verify(fixture.domainRepository, never()).insert(any());
        verify(fixture.fieldRepository, never()).insert(any());
    }

    @Test
    void applyRestore_toNewProjectCreatesAssetsAndRestoreRecord() {
        Fixture fixture = new Fixture();
        fixture.withSourceProject();
        Domain sourceDomain = domain("order", 1L);
        Field sourceField = field("order_no", 1L);
        sourceField.setDomainId(sourceDomain.getId());
        when(fixture.domainRepository.findByProjectId(1L)).thenReturn(List.of(sourceDomain));
        when(fixture.fieldRepository.findAllByProjectId(1L)).thenReturn(List.of(sourceField));
        fixture.withEmptyRemainingAssets();
        when(fixture.projectRepository.existsByName("源项目")).thenReturn(false);
        when(fixture.projectService.create(any(Project.class), eq(false))).thenAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            project.setId(2L);
            return project;
        });
        when(fixture.domainRepository.findByProjectId(2L)).thenReturn(List.of());
        when(fixture.fieldRepository.findAllByProjectId(2L)).thenReturn(List.of());
        when(fixture.enumDictRepository.findDictsByProjectId(2L)).thenReturn(List.of());
        when(fixture.ruleConfigRepository.findByProjectId(2L)).thenReturn(List.of());
        when(fixture.templateRepository.findByProjectId(2L)).thenReturn(List.of());
        when(fixture.standardSnapshotRepository.findByProjectId(2L)).thenReturn(List.of());
        when(fixture.domainRepository.insert(any(Domain.class))).thenAnswer(invocation -> {
            Domain domain = invocation.getArgument(0);
            domain.setId(20L);
            return 1;
        });
        when(fixture.fieldRepository.insert(any(Field.class))).thenAnswer(invocation -> {
            Field field = invocation.getArgument(0);
            field.setId(200L);
            return 1;
        });
        ProjectBackupPackage exported = fixture.service().exportPackage(1L);

        fixture.service().applyRestore(new ProjectRestoreReq(null, false, exported));

        verify(fixture.domainRepository).insert(any(Domain.class));
        verify(fixture.fieldRepository).insert(any(Field.class));
        verify(fixture.restoreRecordRepository).insert(any());
    }

    @Test
    void applyRestore_withOverwriteUpdatesExistingField() {
        Fixture fixture = new Fixture();
        fixture.withSourceProject();
        Field sourceField = field("order_no", 1L);
        sourceField.setComment("来源字段注释");
        when(fixture.domainRepository.findByProjectId(1L)).thenReturn(List.of());
        when(fixture.fieldRepository.findAllByProjectId(1L)).thenReturn(List.of(sourceField));
        fixture.withEmptyRemainingAssets();
        ProjectBackupPackage exported = fixture.service().exportPackage(1L);
        Project target = project(2L, "目标项目");
        Field existing = field("order_no", 2L);
        existing.setId(200L);
        existing.setComment("旧注释");
        when(fixture.projectService.getById(2L)).thenReturn(target);
        when(fixture.domainRepository.findByProjectId(2L)).thenReturn(List.of());
        when(fixture.fieldRepository.findAllByProjectId(2L)).thenReturn(List.of(existing));
        when(fixture.enumDictRepository.findDictsByProjectId(2L)).thenReturn(List.of());
        when(fixture.ruleConfigRepository.findByProjectId(2L)).thenReturn(List.of());
        when(fixture.templateRepository.findByProjectId(2L)).thenReturn(List.of());
        when(fixture.standardSnapshotRepository.findByProjectId(2L)).thenReturn(List.of());

        fixture.service().applyRestore(new ProjectRestoreReq(2L, true, exported));

        verify(fixture.fieldRepository).update(existing);
        assertEquals("来源字段注释", existing.getComment());
        verify(fixture.restoreRecordRepository).insert(any());
    }

    private static com.dataspec.changelog.entity.StandardChangeLog changeLogWithSecret() {
        com.dataspec.changelog.entity.StandardChangeLog log = new com.dataspec.changelog.entity.StandardChangeLog();
        log.setId(1L);
        log.setProjectId(1L);
        log.setTargetType("field");
        log.setAfterJson("{\"password\":\"super-secret\",\"url\":\"jdbc:postgresql://localhost/db\"}");
        log.setChangedAt(LocalDateTime.parse("2026-06-27T10:00:00"));
        return log;
    }

    private static Project project(Long id, String name) {
        Project project = new Project();
        project.setId(id);
        project.setName(name);
        project.setDbType("postgresql");
        return project;
    }

    private static Domain domain(String code, Long projectId) {
        Domain domain = new Domain();
        domain.setId(projectId * 10);
        domain.setProjectId(projectId);
        domain.setCode(code);
        domain.setName(code);
        return domain;
    }

    private static Field field(String name, Long projectId) {
        Field field = new Field();
        field.setId(projectId * 100);
        field.setProjectId(projectId);
        field.setName(name);
        field.setDataType("varchar");
        return field;
    }

    private static final class Fixture {
        private final ProjectRepository projectRepository = mock(ProjectRepository.class);
        private final ProjectService projectService = mock(ProjectService.class);
        private final DomainRepository domainRepository = mock(DomainRepository.class);
        private final FieldRepository fieldRepository = mock(FieldRepository.class);
        private final EnumDictRepository enumDictRepository = mock(EnumDictRepository.class);
        private final RuleConfigRepository ruleConfigRepository = mock(RuleConfigRepository.class);
        private final RuleConfigService ruleConfigService = mock(RuleConfigService.class);
        private final RuleBaselineService ruleBaselineService = mock(RuleBaselineService.class);
        private final TemplateRepository templateRepository = mock(TemplateRepository.class);
        private final StandardSnapshotRepository standardSnapshotRepository = mock(StandardSnapshotRepository.class);
        private final ReverseImportBatchRepository reverseImportBatchRepository = mock(ReverseImportBatchRepository.class);
        private final FieldSourceRepository fieldSourceRepository = mock(FieldSourceRepository.class);
        private final StandardChangeLogRepository changeLogRepository = mock(StandardChangeLogRepository.class);
        private final ProjectRestoreRecordRepository restoreRecordRepository = mock(ProjectRestoreRecordRepository.class);

        void withSourceProject() {
            when(projectService.getById(1L)).thenReturn(project(1L, "源项目"));
        }

        void withEmptyAssets() {
            when(domainRepository.findByProjectId(1L)).thenReturn(List.of());
            when(fieldRepository.findAllByProjectId(1L)).thenReturn(List.of());
            withEmptyRemainingAssets();
        }

        void withEmptyRemainingAssets() {
            when(enumDictRepository.findDictsByProjectId(1L)).thenReturn(List.of());
            when(ruleConfigRepository.findByProjectId(1L)).thenReturn(List.of());
            when(ruleBaselineService.exportBaseline(1L)).thenReturn(emptyRuleBaseline());
            when(templateRepository.findByProjectId(1L)).thenReturn(List.of());
            when(standardSnapshotRepository.findByProjectId(1L)).thenReturn(List.of());
            when(reverseImportBatchRepository.findByProjectId(1L)).thenReturn(List.of());
            when(fieldSourceRepository.findByProjectId(1L)).thenReturn(List.of());
            when(changeLogRepository.findByProjectId(1L, 200)).thenReturn(List.of());
        }

        ProjectBackupServiceImpl service() {
            return new ProjectBackupServiceImpl(
                    projectRepository,
                    projectService,
                    domainRepository,
                    fieldRepository,
                    enumDictRepository,
                    ruleConfigRepository,
                    ruleConfigService,
                    ruleBaselineService,
                    templateRepository,
                    standardSnapshotRepository,
                    reverseImportBatchRepository,
                    fieldSourceRepository,
                    changeLogRepository,
                    restoreRecordRepository,
                    new ObjectMapper());
        }

        private RuleBaselinePackage emptyRuleBaseline() {
            return new RuleBaselinePackage(
                    1,
                    new RuleBaselineInfo(1L, "custom", "自定义规则", "unversioned", "inferred", null, 0),
                    LocalDateTime.parse("2026-06-27T10:00:00"),
                    List.of());
        }
    }
}
