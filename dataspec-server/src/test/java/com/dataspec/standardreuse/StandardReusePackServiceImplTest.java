package com.dataspec.standardreuse;

import com.dataspec.common.exception.BizException;
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
import com.dataspec.standardreuse.entity.StandardReusePack;
import com.dataspec.standardreuse.entity.StandardReusePackApplication;
import com.dataspec.standardreuse.model.StandardReusePackApplyReq;
import com.dataspec.standardreuse.model.StandardReusePackCreateReq;
import com.dataspec.standardreuse.model.StandardReusePackDriftCounts;
import com.dataspec.standardreuse.model.StandardReusePackPlan;
import com.dataspec.standardreuse.repository.StandardReusePackApplicationRepository;
import com.dataspec.standardreuse.repository.StandardReusePackRepository;
import com.dataspec.standardreuse.service.impl.StandardReusePackServiceImpl;
import com.dataspec.template.entity.Template;
import com.dataspec.template.entity.TemplateField;
import com.dataspec.template.repository.TemplateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StandardReusePackServiceImplTest {

    @Test
    void createPack_storesDeterministicPayloadHashAndAssetCounts() throws Exception {
        Fixture fixture = new Fixture();
        fixture.withSourceProjectAssets();
        when(fixture.packRepository.existsByProjectIdAndKeyAndVersion(1L, "shared_core", "2026.07")).thenReturn(false);
        StandardReusePackServiceImpl service = fixture.service();

        var detail = service.createPack(new StandardReusePackCreateReq(
                1L,
                " shared_core ",
                "通用交易标准",
                " 2026.07 ",
                "用户、订单和支付共享字段"));

        ArgumentCaptor<StandardReusePack> captor = ArgumentCaptor.forClass(StandardReusePack.class);
        verify(fixture.packRepository).insert(captor.capture());
        StandardReusePack saved = captor.getValue();
        assertEquals("shared_core", saved.getPackKey());
        assertEquals("2026.07", saved.getBasePackVersion());
        assertEquals(64, saved.getPackageHash().length());
        assertEquals(saved.getPackageHash(), detail.info().packageHash());
        assertEquals(1, detail.info().assetCounts().fields());
        assertEquals(1, detail.info().assetCounts().templates());
        assertFalse(saved.getPayloadJson().contains("\"id\""));
        assertTrue(saved.getPayloadJson().contains("order_no"));
        assertTrue(saved.getPayloadJson().contains("统计订单实付金额"));
        assertTrue(saved.getPayloadJson().contains("展示金额时不要直接输出分单位"));
        assertTrue(saved.getAssetCountsJson().contains("\"fields\":1"));
    }

    @Test
    void createPack_redactsSensitivePayloadBeforePersistingAndReturningDetail() {
        Fixture fixture = new Fixture();
        fixture.withSensitiveSourceProjectAssets();
        when(fixture.packRepository.existsByProjectIdAndKeyAndVersion(1L, "shared_secure", "2026.07")).thenReturn(false);

        var detail = fixture.service().createPack(new StandardReusePackCreateReq(
                1L,
                "shared_secure",
                "通用安全字段 password=raw-pass",
                "2026.07",
                "Authorization: Bearer raw-token"));

        ArgumentCaptor<StandardReusePack> captor = ArgumentCaptor.forClass(StandardReusePack.class);
        verify(fixture.packRepository).insert(captor.capture());
        String payloadJson = captor.getValue().getPayloadJson();
        assertFalse(payloadJson.contains("raw-pass"));
        assertFalse(payloadJson.contains("raw-token"));
        assertFalse(payloadJson.contains("jdbc:mysql://example.invalid/order"));
        assertTrue(payloadJson.contains("[REDACTED]"));
        assertEquals(payloadJson, detail.payloadJson());
    }

    @Test
    void previewApply_reportsCreateSkipOverrideAndDriftWithoutWriting() {
        Fixture fixture = new Fixture();
        StandardReusePack pack = fixture.savedPack();
        when(fixture.packRepository.findById(10L)).thenReturn(Optional.of(pack));
        when(fixture.projectService.getById(2L)).thenReturn(project(2L, "目标项目"));
        Domain domain = domain("order", 2L);
        EnumDict enumDict = enumDict("order_status", 2L);
        enumDict.setName("order_status");
        Field packSourcedDrift = field("order_no", 2L);
        packSourcedDrift.setTags("order,pack:shared_core@2026.07");
        packSourcedDrift.setLength(128);
        when(fixture.domainRepository.findByProjectId(2L)).thenReturn(List.of(domain));
        when(fixture.enumDictRepository.findDictsByProjectId(2L)).thenReturn(List.of(enumDict));
        when(fixture.enumDictRepository.findValuesByEnumId(enumDict.getId()))
                .thenReturn(List.of(enumValue("PAID", enumDict.getId(), "已支付")));
        when(fixture.fieldRepository.findAllByProjectId(2L)).thenReturn(List.of(packSourcedDrift));
        when(fixture.ruleConfigRepository.findByProjectId(2L)).thenReturn(List.of());
        when(fixture.templateRepository.findByProjectId(2L)).thenReturn(List.of());

        StandardReusePackPlan plan = fixture.service().previewApply(new StandardReusePackApplyReq(10L, 2L, false));

        assertEquals(2, plan.counts().created());
        assertEquals(1, plan.counts().overridden());
        assertEquals(1, plan.counts().drifted());
        assertTrue(plan.items().stream().anyMatch(item -> "field".equals(item.assetType())
                && "order_no".equals(item.key())
                && "DRIFTED".equals(item.action())));
        assertTrue(plan.items().stream().anyMatch(item -> "enum_dict".equals(item.assetType())
                && "order_status".equals(item.key())
                && "OVERRIDDEN".equals(item.action())));
        verify(fixture.fieldRepository, never()).insert(any(Field.class));
        verify(fixture.applicationRepository, never()).insert(any(StandardReusePackApplication.class));
    }

    @Test
    void previewApply_marksExistingLocalFieldDifferenceAsOverride() {
        Fixture fixture = new Fixture();
        StandardReusePack pack = fixture.savedPack();
        when(fixture.packRepository.findById(10L)).thenReturn(Optional.of(pack));
        when(fixture.projectService.getById(2L)).thenReturn(project(2L, "目标项目"));
        Field localOverride = field("order_no", 2L);
        localOverride.setLength(128);
        when(fixture.domainRepository.findByProjectId(2L)).thenReturn(List.of());
        when(fixture.enumDictRepository.findDictsByProjectId(2L)).thenReturn(List.of());
        when(fixture.fieldRepository.findAllByProjectId(2L)).thenReturn(List.of(localOverride));
        when(fixture.ruleConfigRepository.findByProjectId(2L)).thenReturn(List.of());
        when(fixture.templateRepository.findByProjectId(2L)).thenReturn(List.of());

        StandardReusePackPlan plan = fixture.service().previewApply(new StandardReusePackApplyReq(10L, 2L, false));

        assertEquals(1, plan.counts().overridden());
        assertTrue(plan.items().stream().anyMatch(item -> "field".equals(item.assetType())
                && "order_no".equals(item.key())
                && "OVERRIDDEN".equals(item.action())));
    }

    @Test
    void applyPack_createsMissingAssetsMarksFieldSourceAndStoresPostApplyDrift() throws Exception {
        Fixture fixture = new Fixture();
        StandardReusePack pack = fixture.savedPack();
        when(fixture.packRepository.findById(10L)).thenReturn(Optional.of(pack));
        when(fixture.projectService.getById(2L)).thenReturn(project(2L, "目标项目"));
        MutableTargetProject targetProject = new MutableTargetProject(2L);
        targetProject.install(fixture);

        var result = fixture.service().applyPack(new StandardReusePackApplyReq(10L, 2L, false));

        assertEquals(5, result.plan().counts().created());
        ArgumentCaptor<Field> fieldCaptor = ArgumentCaptor.forClass(Field.class);
        verify(fixture.fieldRepository).insert(fieldCaptor.capture());
        Field createdField = fieldCaptor.getValue();
        assertTrue(createdField.getTags().contains("pack:shared_core@2026.07"));
        assertEquals("统计订单实付金额", createdField.getPreferredUseCases());
        assertEquals("展示金额时不要直接输出分单位", createdField.getAvoidWhen());
        assertEquals("orders.id = payments.order_id", createdField.getJoinHints());
        assertEquals("payment_status = 'PAID'", createdField.getDefaultFilters());
        assertEquals("sum(amount_cent) / 100", createdField.getAggregationHints());
        assertEquals("展示层改用 amount_yuan", createdField.getReplacementGuidance());
        assertEquals("把 amount_cent 当元展示", createdField.getMisuseExamples());
        ArgumentCaptor<StandardReusePackApplication> applicationCaptor =
                ArgumentCaptor.forClass(StandardReusePackApplication.class);
        verify(fixture.applicationRepository).insert(applicationCaptor.capture());
        StandardReusePackApplication application = applicationCaptor.getValue();
        StandardReusePackDriftCounts postApplyCounts = fixture.objectMapper.readValue(
                application.getDriftCountsJson(),
                StandardReusePackDriftCounts.class);
        assertEquals(0, postApplyCounts.missing());
        assertEquals(5, postApplyCounts.matched());
        assertFalse(application.getDriftReportJson().contains("\"MISSING\""));
    }

    @Test
    void applyPack_refreshesFieldsAfterNameReservation() {
        Fixture fixture = new Fixture();
        StandardReusePack pack = fixture.savedPack();
        when(fixture.packRepository.findById(10L)).thenReturn(Optional.of(pack));
        when(fixture.projectService.getById(2L)).thenReturn(project(2L, "目标项目"));
        MutableTargetProject targetProject = new MutableTargetProject(2L);
        targetProject.install(fixture);
        Field concurrentlyCreated = field("order_no", 2L);
        concurrentlyCreated.setId(901L);
        doAnswer(invocation -> {
            targetProject.fields.add(concurrentlyCreated);
            return null;
        }).when(fixture.fieldNameReservationGuard).reserveAll(eq(2L), anyCollection());

        fixture.service().applyPack(new StandardReusePackApplyReq(10L, 2L, false));

        verify(fixture.fieldRepository, never()).insert(any(Field.class));
        assertEquals(List.of("order_no"), targetProject.fields.stream().map(Field::getName).toList());
    }

    @Test
    void previewApply_rejectsMissingPackBeforeWriting() {
        Fixture fixture = new Fixture();
        when(fixture.packRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(BizException.class,
                () -> fixture.service().previewApply(new StandardReusePackApplyReq(404L, 2L, false)));

        verify(fixture.fieldRepository, never()).insert(any(Field.class));
        verify(fixture.applicationRepository, never()).insert(any(StandardReusePackApplication.class));
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

    private static EnumDict enumDict(String code, Long projectId) {
        EnumDict dict = new EnumDict();
        dict.setId(projectId * 20);
        dict.setProjectId(projectId);
        dict.setCode(code);
        dict.setName(code);
        dict.setValueType("string");
        return dict;
    }

    private static EnumValue enumValue(String value, Long enumId) {
        return enumValue(value, enumId, value);
    }

    private static EnumValue enumValue(String value, Long enumId, String label) {
        EnumValue enumValue = new EnumValue();
        enumValue.setId(enumId * 10);
        enumValue.setEnumId(enumId);
        enumValue.setValue(value);
        enumValue.setLabel(label);
        enumValue.setSortOrder(10);
        return enumValue;
    }

    private static Field field(String name, Long projectId) {
        Field field = new Field();
        field.setId(projectId * 100);
        field.setProjectId(projectId);
        field.setName(name);
        field.setDisplayName("订单号");
        field.setDataType("varchar");
        field.setNullable(false);
        field.setComment("来源项目订单号");
        field.setTags("order");
        field.setPreferredUseCases("统计订单实付金额");
        field.setAvoidWhen("展示金额时不要直接输出分单位");
        field.setJoinHints("orders.id = payments.order_id");
        field.setDefaultFilters("payment_status = 'PAID'");
        field.setAggregationHints("sum(amount_cent) / 100");
        field.setReplacementGuidance("展示层改用 amount_yuan");
        field.setMisuseExamples("把 amount_cent 当元展示");
        return field;
    }

    private static RuleConfig rule(String ruleCode, Long projectId) {
        RuleConfig rule = new RuleConfig();
        rule.setId(projectId * 30);
        rule.setProjectId(projectId);
        rule.setRuleCode(ruleCode);
        rule.setRuleName(ruleCode);
        rule.setSeverity("ERROR");
        rule.setEnabled(true);
        rule.setParamsJson("{}");
        return rule;
    }

    private static Template template(String name, Long projectId) {
        Template template = new Template();
        template.setId(projectId * 40);
        template.setProjectId(projectId);
        template.setName(name);
        template.setTablePrefix("t_order");
        return template;
    }

    private static TemplateField templateField(Long templateId, Long fieldId) {
        TemplateField field = new TemplateField();
        field.setId(templateId * 10);
        field.setTemplateId(templateId);
        field.setFieldId(fieldId);
        field.setName("order_no");
        field.setDataType("varchar");
        field.setNullable(false);
        field.setSortOrder(10);
        field.setIsRequired(true);
        return field;
    }

    private static void assignDomainIds(DomainRepository repository) {
        AtomicLong ids = new AtomicLong(20);
        when(repository.insert(any(Domain.class))).thenAnswer(invocation -> {
            Domain domain = invocation.getArgument(0);
            domain.setId(ids.getAndIncrement());
            return 1;
        });
    }

    private static void assignEnumIds(EnumDictRepository repository) {
        AtomicLong dictIds = new AtomicLong(30);
        AtomicLong valueIds = new AtomicLong(300);
        when(repository.insertDict(any(EnumDict.class))).thenAnswer(invocation -> {
            EnumDict dict = invocation.getArgument(0);
            dict.setId(dictIds.getAndIncrement());
            return 1;
        });
        when(repository.insertValue(any(EnumValue.class))).thenAnswer(invocation -> {
            EnumValue value = invocation.getArgument(0);
            value.setId(valueIds.getAndIncrement());
            return 1;
        });
    }

    private static void assignFieldIds(FieldRepository repository) {
        AtomicLong ids = new AtomicLong(100);
        when(repository.insert(any(Field.class))).thenAnswer(invocation -> {
            Field field = invocation.getArgument(0);
            field.setId(ids.getAndIncrement());
            return 1;
        });
    }

    private static void assignTemplateIds(TemplateRepository repository) {
        AtomicLong templateIds = new AtomicLong(400);
        AtomicLong fieldIds = new AtomicLong(500);
        when(repository.insert(any(Template.class))).thenAnswer(invocation -> {
            Template template = invocation.getArgument(0);
            template.setId(templateIds.getAndIncrement());
            return 1;
        });
        when(repository.insertField(any(TemplateField.class))).thenAnswer(invocation -> {
            TemplateField field = invocation.getArgument(0);
            field.setId(fieldIds.getAndIncrement());
            return 1;
        });
    }

    private static final class Fixture {
        private final ProjectService projectService = mock(ProjectService.class);
        private final DomainRepository domainRepository = mock(DomainRepository.class);
        private final FieldRepository fieldRepository = mock(FieldRepository.class);
        private final EnumDictRepository enumDictRepository = mock(EnumDictRepository.class);
        private final RuleConfigRepository ruleConfigRepository = mock(RuleConfigRepository.class);
        private final TemplateRepository templateRepository = mock(TemplateRepository.class);
        private final ProjectFieldNameReservationGuard fieldNameReservationGuard =
                mock(ProjectFieldNameReservationGuard.class);
        private final StandardReusePackRepository packRepository = mock(StandardReusePackRepository.class);
        private final StandardReusePackApplicationRepository applicationRepository =
                mock(StandardReusePackApplicationRepository.class);
        private final ObjectMapper objectMapper = new ObjectMapper();

        void withSourceProjectAssets() {
            when(projectService.getById(1L)).thenReturn(project(1L, "源项目"));
            Domain domain = domain("order", 1L);
            EnumDict dict = enumDict("order_status", 1L);
            Field field = field("order_no", 1L);
            field.setDomainId(domain.getId());
            field.setCodeSetId(dict.getId());
            Template template = template("订单表模板", 1L);
            when(domainRepository.findByProjectId(1L)).thenReturn(List.of(domain));
            when(enumDictRepository.findDictsByProjectId(1L)).thenReturn(List.of(dict));
            when(enumDictRepository.findValuesByEnumId(dict.getId())).thenReturn(List.of(enumValue("PAID", dict.getId())));
            when(fieldRepository.findAllByProjectId(1L)).thenReturn(List.of(field));
            when(ruleConfigRepository.findByProjectId(1L)).thenReturn(List.of(rule("required_columns", 1L)));
            when(templateRepository.findByProjectId(1L)).thenReturn(List.of(template));
            when(templateRepository.findFieldsByTemplateId(template.getId()))
                    .thenReturn(List.of(templateField(template.getId(), field.getId())));
        }

        void withSensitiveSourceProjectAssets() {
            when(projectService.getById(1L)).thenReturn(project(1L, "源项目"));
            Domain domain = domain("secure", 1L);
            EnumDict dict = enumDict("secret_kind", 1L);
            Field field = field("api_token", 1L);
            field.setDomainId(domain.getId());
            field.setCodeSetId(dict.getId());
            field.setSensitive(true);
            field.setDefaultValue("password=raw-pass");
            field.setComment("Authorization: Bearer raw-token");
            field.setExampleValue("jdbc:mysql://example.invalid/order?password=raw-pass");
            field.setValidExamplesJson("[\"Authorization: Bearer raw-token\"]");
            field.setInvalidExamplesJson("[\"token=raw-token\"]");
            field.setFormatNotes("jdbc:mysql://example.invalid/order");
            RuleConfig rule = rule("secret_rule", 1L);
            rule.setParamsJson("{\"password\":\"raw-pass\",\"authorization\":\"Bearer raw-token\"}");
            Template template = template("安全字段模板", 1L);
            TemplateField templateField = templateField(template.getId(), field.getId());
            templateField.setDefaultValue("token=raw-token");
            templateField.setComment("jdbc:mysql://example.invalid/order");
            when(domainRepository.findByProjectId(1L)).thenReturn(List.of(domain));
            when(enumDictRepository.findDictsByProjectId(1L)).thenReturn(List.of(dict));
            when(enumDictRepository.findValuesByEnumId(dict.getId())).thenReturn(List.of(enumValue("TOKEN", dict.getId())));
            when(fieldRepository.findAllByProjectId(1L)).thenReturn(List.of(field));
            when(ruleConfigRepository.findByProjectId(1L)).thenReturn(List.of(rule));
            when(templateRepository.findByProjectId(1L)).thenReturn(List.of(template));
            when(templateRepository.findFieldsByTemplateId(template.getId())).thenReturn(List.of(templateField));
        }

        StandardReusePack savedPack() {
            withSourceProjectAssets();
            var detail = service().createPack(new StandardReusePackCreateReq(
                    1L,
                    "shared_core",
                    "通用交易标准",
                    "2026.07",
                    "用户、订单和支付共享字段"));
            StandardReusePack pack = new StandardReusePack();
            pack.setId(10L);
            pack.setProjectId(1L);
            pack.setSourceProjectName("源项目");
            pack.setPackKey(detail.info().packKey());
            pack.setPackName(detail.info().packName());
            pack.setBasePackVersion(detail.info().basePackVersion());
            pack.setDescription(detail.info().description());
            pack.setPackageHash(detail.info().packageHash());
            pack.setPayloadJson(detail.payloadJson());
            pack.setAssetCountsJson(objectMapper.valueToTree(detail.info().assetCounts()).toString());
            return pack;
        }

        StandardReusePackServiceImpl service() {
            return new StandardReusePackServiceImpl(
                    projectService,
                    domainRepository,
                    fieldRepository,
                    fieldNameReservationGuard,
                    enumDictRepository,
                    ruleConfigRepository,
                    templateRepository,
                    packRepository,
                    applicationRepository,
                    objectMapper);
        }
    }

    private static final class MutableTargetProject {
        private final Long projectId;
        private final List<Domain> domains = new ArrayList<>();
        private final List<EnumDict> enums = new ArrayList<>();
        private final Map<Long, List<EnumValue>> enumValuesById = new HashMap<>();
        private final List<Field> fields = new ArrayList<>();
        private final List<RuleConfig> rules = new ArrayList<>();
        private final List<Template> templates = new ArrayList<>();
        private final Map<Long, List<TemplateField>> templateFieldsById = new HashMap<>();
        private final AtomicLong domainIds = new AtomicLong(20);
        private final AtomicLong enumIds = new AtomicLong(30);
        private final AtomicLong enumValueIds = new AtomicLong(300);
        private final AtomicLong fieldIds = new AtomicLong(100);
        private final AtomicLong ruleIds = new AtomicLong(200);
        private final AtomicLong templateIds = new AtomicLong(400);
        private final AtomicLong templateFieldIds = new AtomicLong(500);

        private MutableTargetProject(Long projectId) {
            this.projectId = projectId;
        }

        private void install(Fixture fixture) {
            when(fixture.domainRepository.findByProjectId(projectId)).thenAnswer(invocation -> domains);
            when(fixture.domainRepository.insert(any(Domain.class))).thenAnswer(invocation -> {
                Domain domain = invocation.getArgument(0);
                domain.setId(domainIds.getAndIncrement());
                domains.add(domain);
                return 1;
            });
            when(fixture.enumDictRepository.findDictsByProjectId(projectId)).thenAnswer(invocation -> enums);
            when(fixture.enumDictRepository.insertDict(any(EnumDict.class))).thenAnswer(invocation -> {
                EnumDict dict = invocation.getArgument(0);
                dict.setId(enumIds.getAndIncrement());
                enums.add(dict);
                enumValuesById.put(dict.getId(), new ArrayList<>());
                return 1;
            });
            when(fixture.enumDictRepository.findValuesByEnumId(anyLong()))
                    .thenAnswer(invocation -> enumValuesById.getOrDefault(invocation.getArgument(0), List.of()));
            when(fixture.enumDictRepository.existsValueByEnumIdAndValue(anyLong(), any())).thenAnswer(invocation ->
                    enumValuesById.getOrDefault(invocation.getArgument(0), List.of()).stream()
                            .anyMatch(value -> value.getValue().equals(invocation.getArgument(1))));
            when(fixture.enumDictRepository.insertValue(any(EnumValue.class))).thenAnswer(invocation -> {
                EnumValue value = invocation.getArgument(0);
                value.setId(enumValueIds.getAndIncrement());
                enumValuesById.computeIfAbsent(value.getEnumId(), id -> new ArrayList<>()).add(value);
                return 1;
            });
            when(fixture.fieldRepository.findAllByProjectId(projectId)).thenAnswer(invocation -> fields);
            when(fixture.fieldRepository.findByNamesInProject(anyCollection(), eq(projectId)))
                    .thenAnswer(invocation -> {
                        Collection<String> names = invocation.getArgument(0);
                        return fields.stream().filter(field -> names.contains(field.getName())).toList();
                    });
            when(fixture.fieldRepository.insert(any(Field.class))).thenAnswer(invocation -> {
                Field field = invocation.getArgument(0);
                field.setId(fieldIds.getAndIncrement());
                fields.add(field);
                return 1;
            });
            when(fixture.ruleConfigRepository.findByProjectId(projectId)).thenAnswer(invocation -> rules);
            when(fixture.ruleConfigRepository.insert(any(RuleConfig.class))).thenAnswer(invocation -> {
                RuleConfig rule = invocation.getArgument(0);
                rule.setId(ruleIds.getAndIncrement());
                rules.add(rule);
                return 1;
            });
            when(fixture.templateRepository.findByProjectId(projectId)).thenAnswer(invocation -> templates);
            when(fixture.templateRepository.insert(any(Template.class))).thenAnswer(invocation -> {
                Template template = invocation.getArgument(0);
                template.setId(templateIds.getAndIncrement());
                templates.add(template);
                templateFieldsById.put(template.getId(), new ArrayList<>());
                return 1;
            });
            when(fixture.templateRepository.findFieldsByTemplateId(anyLong()))
                    .thenAnswer(invocation -> templateFieldsById.getOrDefault(invocation.getArgument(0), List.of()));
            when(fixture.templateRepository.insertField(any(TemplateField.class))).thenAnswer(invocation -> {
                TemplateField field = invocation.getArgument(0);
                field.setId(templateFieldIds.getAndIncrement());
                templateFieldsById.computeIfAbsent(field.getTemplateId(), id -> new ArrayList<>()).add(field);
                return 1;
            });
        }
    }
}
