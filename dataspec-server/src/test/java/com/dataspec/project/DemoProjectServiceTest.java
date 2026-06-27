package com.dataspec.project;

import com.dataspec.field.entity.Field;
import com.dataspec.field.repository.FieldRepository;
import com.dataspec.project.entity.Project;
import com.dataspec.project.model.DemoProjectResult;
import com.dataspec.project.repository.ProjectRepository;
import com.dataspec.project.service.DemoProjectService;
import com.dataspec.project.service.ProjectService;
import com.dataspec.rulebaseline.service.BuiltInRuleBaselines;
import com.dataspec.rulebaseline.service.RuleBaselineService;
import com.dataspec.standards.BuiltInStandardsImportService;
import com.dataspec.template.entity.Template;
import com.dataspec.template.entity.TemplateField;
import com.dataspec.template.repository.TemplateRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DemoProjectServiceTest {

    @Test
    void createOrReuseDemoProject_createsProjectAndSeedsAssets() {
        ProjectRepository projectRepository = mock(ProjectRepository.class);
        ProjectService projectService = mock(ProjectService.class);
        BuiltInStandardsImportService standardsImportService = mock(BuiltInStandardsImportService.class);
        FieldRepository fieldRepository = mock(FieldRepository.class);
        TemplateRepository templateRepository = mock(TemplateRepository.class);
        RuleBaselineService ruleBaselineService = mock(RuleBaselineService.class);
        when(projectRepository.findByName(DemoProjectService.DEMO_PROJECT_NAME)).thenReturn(Optional.empty());
        when(projectService.create(any(Project.class), eq(true))).thenAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            project.setId(11L);
            return project;
        });
        when(templateRepository.findByProjectId(11L)).thenReturn(List.of());
        when(templateRepository.findFieldsByTemplateId(22L)).thenReturn(List.of());
        doAnswer(invocation -> {
            Template template = invocation.getArgument(0);
            template.setId(22L);
            return 1;
        }).when(templateRepository).insert(any(Template.class));
        when(fieldRepository.findAllByProjectId(11L)).thenReturn(demoFields());
        DemoProjectService service = new DemoProjectService(
                projectRepository,
                projectService,
                standardsImportService,
                fieldRepository,
                templateRepository,
                ruleBaselineService);

        DemoProjectResult result = service.createOrReuseDemoProject();

        assertTrue(result.created());
        assertEquals(11L, result.project().getId());
        assertEquals(22L, result.templateId());
        assertEquals("user_order", result.sampleTableName());
        assertTrue(result.badExampleSql().contains("UserOrder"));
        assertTrue(result.goodExampleSql().contains("user_order"));
        verify(projectService).create(argThat(project ->
                DemoProjectService.DEMO_PROJECT_NAME.equals(project.getName())
                        && "postgresql".equals(project.getDbType())), eq(true));
        verify(standardsImportService, never()).importBuiltInStandards(11L);
        verify(ruleBaselineService, never()).applyBuiltInBaseline(anyLong(), anyString(), anyBoolean());
        verify(templateRepository, times(10)).insertField(any(TemplateField.class));
    }

    @Test
    void createOrReuseDemoProject_reusesProjectAndBackfillsMissingAssets() {
        ProjectRepository projectRepository = mock(ProjectRepository.class);
        ProjectService projectService = mock(ProjectService.class);
        BuiltInStandardsImportService standardsImportService = mock(BuiltInStandardsImportService.class);
        FieldRepository fieldRepository = mock(FieldRepository.class);
        TemplateRepository templateRepository = mock(TemplateRepository.class);
        RuleBaselineService ruleBaselineService = mock(RuleBaselineService.class);
        Project existingProject = new Project();
        existingProject.setId(11L);
        existingProject.setName(DemoProjectService.DEMO_PROJECT_NAME);
        Template existingTemplate = new Template();
        existingTemplate.setId(22L);
        existingTemplate.setName("订单表模板");
        when(projectRepository.findByName(DemoProjectService.DEMO_PROJECT_NAME)).thenReturn(Optional.of(existingProject));
        when(templateRepository.findByProjectId(11L)).thenReturn(List.of(existingTemplate));
        when(templateRepository.findFieldsByTemplateId(22L)).thenReturn(List.of(existingTemplateField("id")));
        when(fieldRepository.findAllByProjectId(11L)).thenReturn(demoFields());
        DemoProjectService service = new DemoProjectService(
                projectRepository,
                projectService,
                standardsImportService,
                fieldRepository,
                templateRepository,
                ruleBaselineService);

        DemoProjectResult result = service.createOrReuseDemoProject();

        assertFalse(result.created());
        assertEquals(11L, result.project().getId());
        assertEquals(22L, result.templateId());
        verify(projectService, never()).create(any(Project.class), anyBoolean());
        verify(standardsImportService).importBuiltInStandards(11L);
        verify(ruleBaselineService).applyBuiltInBaseline(11L, BuiltInRuleBaselines.PERSONAL_DEFAULT, false);
        ArgumentCaptor<TemplateField> fieldCaptor = ArgumentCaptor.forClass(TemplateField.class);
        verify(templateRepository, times(9)).insertField(fieldCaptor.capture());
        assertTrue(fieldCaptor.getAllValues().stream().noneMatch(field -> "id".equals(field.getName())));
    }

    private List<Field> demoFields() {
        return List.of(
                field(1L, "id", "bigserial", false, null, "自增主键"),
                field(2L, "user_id", "bigint", false, null, "关联用户表"),
                field(3L, "order_no", "varchar(64)", false, null, "订单唯一编号"),
                field(4L, "mobile_no", "varchar(20)", true, null, "用户手机号"),
                field(5L, "amount_cent", "bigint", false, "0", "金额，以分为单位存储，避免浮点精度问题"),
                field(6L, "status", "integer", false, "0", "业务状态，具体含义参考枚举字典"),
                field(7L, "remark", "text", true, null, "备注信息"),
                field(8L, "created_at", "timestamp with time zone", false, "now()", "记录创建时间"),
                field(9L, "updated_at", "timestamp with time zone", false, "now()", "记录最后更新时间"),
                field(10L, "is_deleted", "boolean", false, "false", "软删除标记")
        );
    }

    private Field field(Long id, String name, String dataType, boolean nullable, String defaultValue, String comment) {
        Field field = new Field();
        field.setId(id);
        field.setName(name);
        field.setDataType(dataType);
        field.setNullable(nullable);
        field.setDefaultValue(defaultValue);
        field.setComment(comment);
        return field;
    }

    private TemplateField existingTemplateField(String name) {
        TemplateField field = new TemplateField();
        field.setName(name);
        return field;
    }
}
