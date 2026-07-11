package com.dataspec.generator;

import com.dataspec.domain.entity.Domain;
import com.dataspec.domain.service.DomainService;
import com.dataspec.enumdict.entity.EnumDict;
import com.dataspec.enumdict.entity.EnumValue;
import com.dataspec.enumdict.service.EnumDictService;
import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.generator.service.MarkdownGeneratorService;
import com.dataspec.template.entity.Template;
import com.dataspec.template.entity.TemplateField;
import com.dataspec.template.service.TemplateService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MarkdownGeneratorServiceTest {

    @Test
    void generateDataDictionary_includesProjectOverviewMetadataAndTemplates() {
        FieldService fieldService = mock(FieldService.class);
        DomainService domainService = mock(DomainService.class);
        EnumDictService enumDictService = mock(EnumDictService.class);
        TemplateService templateService = mock(TemplateService.class);
        com.dataspec.fieldsemantic.service.FieldSemanticRuleService fieldSemanticRuleService =
                mock(com.dataspec.fieldsemantic.service.FieldSemanticRuleService.class);
        com.dataspec.metric.service.MetricDefinitionService metricDefinitionService =
                mock(com.dataspec.metric.service.MetricDefinitionService.class);
        MarkdownGeneratorService service = new MarkdownGeneratorService(
                fieldService,
                domainService,
                enumDictService,
                templateService,
                fieldSemanticRuleService,
                metricDefinitionService);

        when(domainService.listByProject(1L)).thenReturn(List.of(domain(3L, "user", "用户域")));
        when(fieldService.listByProject(1L)).thenReturn(List.of(field()));
        when(enumDictService.listByProject(1L)).thenReturn(List.of(enumDict(6L)));
        when(enumDictService.listValues(6L)).thenReturn(List.of(enumValue()));
        when(templateService.listByProject(1L)).thenReturn(List.of(template(9L)));
        when(templateService.listFields(9L)).thenReturn(List.of(templateField()));
        when(fieldSemanticRuleService.list(1L, null, null, null)).thenReturn(List.of());
        when(metricDefinitionService.list(1L, null, null, null)).thenReturn(List.of());

        String markdown = service.generateDataDictionary(1L);

        assertTrue(markdown.contains("## 概览"));
        assertTrue(markdown.contains("| 数据域 | 1 |"));
        assertTrue(markdown.contains("| 标准字段 | 1 |"));
        assertTrue(markdown.contains("| 枚举字典 | 1 |"));
        assertTrue(markdown.contains("| 表模板 | 1 |"));
        assertTrue(markdown.contains("用户域(user)"));
        assertTrue(markdown.contains("phone,mobile"));
        assertTrue(markdown.contains("contact"));
        assertTrue(markdown.contains("是"));
        assertTrue(markdown.contains("deprecated"));
        assertTrue(markdown.contains("13800138000"));
        assertTrue(markdown.contains("值类型：`integer`"));
        assertTrue(markdown.contains("## 表模板"));
        assertTrue(markdown.contains("### 用户模板 (`tpl_user`)"));
        assertTrue(markdown.contains("| mobile_no | varchar(20) | 是 | 否 | - | 10 | 4 | 用户手机号 |"));
    }

    private Domain domain(Long id, String code, String name) {
        Domain domain = new Domain();
        domain.setId(id);
        domain.setCode(code);
        domain.setName(name);
        domain.setDescription("用户资料");
        return domain;
    }

    private Field field() {
        Field field = new Field();
        field.setName("mobile_no");
        field.setDisplayName("手机号");
        field.setDataType("varchar(20)");
        field.setNullable(false);
        field.setDefaultValue("");
        field.setComment("用户|手机号");
        field.setDomainId(3L);
        field.setTags("contact,sensitive");
        field.setAliases("phone,mobile");
        field.setCategory("contact");
        field.setCodeSetId(6L);
        field.setSensitive(true);
        field.setStatus("deprecated");
        field.setExampleValue("13800138000");
        return field;
    }

    private EnumDict enumDict(Long id) {
        EnumDict dict = new EnumDict();
        dict.setId(id);
        dict.setCode("user_status");
        dict.setName("用户状态");
        dict.setValueType("integer");
        dict.setDescription("用户状态枚举");
        return dict;
    }

    private EnumValue enumValue() {
        EnumValue value = new EnumValue();
        value.setValue("1");
        value.setLabel("启用");
        value.setSortOrder(10);
        return value;
    }

    private Template template(Long id) {
        Template template = new Template();
        template.setId(id);
        template.setName("用户模板");
        template.setDescription("用户资料表");
        template.setTablePrefix("tpl_user");
        return template;
    }

    private TemplateField templateField() {
        TemplateField field = new TemplateField();
        field.setName("mobile_no");
        field.setDataType("varchar(20)");
        field.setNullable(false);
        field.setDefaultValue("");
        field.setComment("用户手机号");
        field.setFieldId(4L);
        field.setIsRequired(true);
        field.setSortOrder(10);
        return field;
    }
}
