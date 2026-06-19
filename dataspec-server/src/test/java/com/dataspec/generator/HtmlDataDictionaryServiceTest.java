package com.dataspec.generator;

import com.dataspec.domain.entity.Domain;
import com.dataspec.domain.service.DomainService;
import com.dataspec.enumdict.entity.EnumDict;
import com.dataspec.enumdict.entity.EnumValue;
import com.dataspec.enumdict.service.EnumDictService;
import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.generator.service.HtmlDataDictionaryService;
import com.dataspec.template.entity.Template;
import com.dataspec.template.entity.TemplateField;
import com.dataspec.template.service.TemplateService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HtmlDataDictionaryServiceTest {

    @Test
    void generateHtml_includesDictionarySectionsAndEscapesHtml() {
        TestFixture fixture = fixtureWithProjectData();

        String html = fixture.service().generateHtml(1L);

        assertTrue(html.contains("<!doctype html>"));
        assertTrue(html.contains("DataSpec 数据字典"));
        assertTrue(html.contains("用户域 (user)"));
        assertTrue(html.contains("mobile_no"));
        assertTrue(html.contains("用户状态 (user_status)"));
        assertTrue(html.contains("用户模板 (tpl_user)"));
        assertTrue(html.contains("flowchart LR"));
        assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt; &amp; 备注"));
        assertFalse(html.contains("<script>alert(1)</script>"));
    }

    @Test
    void generateMermaid_includesDomainEnumAndTemplateRelations() {
        TestFixture fixture = fixtureWithProjectData();

        String mermaid = fixture.service().generateMermaid(1L);

        assertTrue(mermaid.contains("flowchart LR"));
        assertTrue(mermaid.contains("domain_3 -- \"归属\" --> field_4"));
        assertTrue(mermaid.contains("field_4 -- \"引用\" --> enum_6"));
        assertTrue(mermaid.contains("template_9 -- \"包含\" --> template_field_10"));
        assertTrue(mermaid.contains("template_field_10 -- \"关联标准字段\" --> field_4"));
    }

    @Test
    void generateMermaid_emptyProjectStillReturnsValidGraph() {
        FieldService fieldService = mock(FieldService.class);
        DomainService domainService = mock(DomainService.class);
        EnumDictService enumDictService = mock(EnumDictService.class);
        TemplateService templateService = mock(TemplateService.class);
        HtmlDataDictionaryService service = new HtmlDataDictionaryService(
                fieldService,
                domainService,
                enumDictService,
                templateService);
        when(domainService.listByProject(1L)).thenReturn(List.of());
        when(fieldService.listByProject(1L)).thenReturn(List.of());
        when(enumDictService.listByProject(1L)).thenReturn(List.of());
        when(templateService.listByProject(1L)).thenReturn(List.of());

        String mermaid = service.generateMermaid(1L);

        assertTrue(mermaid.contains("flowchart LR"));
        assertTrue(mermaid.contains("project[\"项目标准\"]"));
        assertFalse(mermaid.contains("null"));
    }

    private TestFixture fixtureWithProjectData() {
        FieldService fieldService = mock(FieldService.class);
        DomainService domainService = mock(DomainService.class);
        EnumDictService enumDictService = mock(EnumDictService.class);
        TemplateService templateService = mock(TemplateService.class);
        HtmlDataDictionaryService service = new HtmlDataDictionaryService(
                fieldService,
                domainService,
                enumDictService,
                templateService);

        when(domainService.listByProject(1L)).thenReturn(List.of(domain()));
        when(fieldService.listByProject(1L)).thenReturn(List.of(field()));
        when(enumDictService.listByProject(1L)).thenReturn(List.of(enumDict()));
        when(enumDictService.listValues(6L)).thenReturn(List.of(enumValue()));
        when(templateService.listByProject(1L)).thenReturn(List.of(template()));
        when(templateService.listFields(9L)).thenReturn(List.of(templateField()));
        return new TestFixture(service);
    }

    private Domain domain() {
        Domain domain = new Domain();
        domain.setId(3L);
        domain.setCode("user");
        domain.setName("用户域");
        domain.setDescription("用户资料");
        return domain;
    }

    private Field field() {
        Field field = new Field();
        field.setId(4L);
        field.setName("mobile_no");
        field.setDisplayName("手机号");
        field.setDataType("varchar(20)");
        field.setNullable(false);
        field.setDefaultValue("");
        field.setComment("<script>alert(1)</script> & 备注");
        field.setDomainId(3L);
        field.setAliases("phone,mobile");
        field.setCategory("contact");
        field.setCodeSetId(6L);
        field.setSensitive(true);
        field.setStatus("enabled");
        field.setExampleValue("13800138000");
        return field;
    }

    private EnumDict enumDict() {
        EnumDict dict = new EnumDict();
        dict.setId(6L);
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

    private Template template() {
        Template template = new Template();
        template.setId(9L);
        template.setName("用户模板");
        template.setDescription("用户资料表");
        template.setTablePrefix("tpl_user");
        return template;
    }

    private TemplateField templateField() {
        TemplateField field = new TemplateField();
        field.setId(10L);
        field.setFieldId(4L);
        field.setName("mobile_no");
        field.setDataType("varchar(20)");
        field.setNullable(false);
        field.setDefaultValue("");
        field.setComment("用户手机号");
        field.setIsRequired(true);
        field.setSortOrder(10);
        return field;
    }

    private record TestFixture(HtmlDataDictionaryService service) {
    }
}
