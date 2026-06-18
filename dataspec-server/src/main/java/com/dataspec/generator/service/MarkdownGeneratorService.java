package com.dataspec.generator.service;

import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.enumdict.entity.EnumDict;
import com.dataspec.enumdict.entity.EnumValue;
import com.dataspec.enumdict.service.EnumDictService;
import com.dataspec.domain.entity.Domain;
import com.dataspec.domain.service.DomainService;
import com.dataspec.template.entity.Template;
import com.dataspec.template.entity.TemplateField;
import com.dataspec.template.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Markdown 数据字典生成器
 */
@Service
@RequiredArgsConstructor
public class MarkdownGeneratorService {

    private final FieldService fieldService;
    private final DomainService domainService;
    private final EnumDictService enumDictService;
    private final TemplateService templateService;

    /**
     * 生成项目级 Markdown 数据字典
     */
    public String generateDataDictionary(Long projectId) {
        StringBuilder md = new StringBuilder();
        md.append("# 数据字典\n\n");

        List<Domain> domains = domainService.listByProject(projectId);
        List<Field> fields = fieldService.listByProject(projectId);
        List<EnumDict> enums = enumDictService.listByProject(projectId);
        List<Template> templates = templateService.listByProject(projectId);
        Map<Long, Domain> domainsById = domainsById(domains);

        appendOverview(md, domains, fields, enums, templates);

        // 数据域
        if (!domains.isEmpty()) {
            md.append("## 数据域\n\n");
            md.append("| 编码 | 名称 | 描述 |\n");
            md.append("|------|------|------|\n");
            for (Domain d : domains) {
                md.append(String.format("| %s | %s | %s |\n",
                        cell(d.getCode()), cell(d.getName()), cell(d.getDescription())));
            }
            md.append("\n");
        }

        // 标准字段
        if (!fields.isEmpty()) {
            md.append("## 标准字段库\n\n");
            md.append("| 字段名 | 显示名 | 数据域 | 数据类型 | 可空 | 默认值 | 敏感 | 状态 | 别名 | 分类 | 代码集 | 示例 | 注释 |\n");
            md.append("|--------|--------|--------|----------|------|--------|------|------|------|------|--------|------|------|\n");
            for (Field f : fields) {
                md.append(String.format("| %s | %s | %s | %s | %s | %s | %s | %s | %s | %s | %s | %s | %s |\n",
                        cell(f.getName()),
                        cell(f.getDisplayName()),
                        cell(domainLabel(f.getDomainId(), domainsById)),
                        cell(f.getDataType()),
                        boolText(f.getNullable()),
                        cell(f.getDefaultValue()),
                        boolText(f.getSensitive()),
                        cell(f.getStatus()),
                        cell(f.getAliases()),
                        cell(f.getCategory()),
                        f.getCodeSetId() != null ? f.getCodeSetId().toString() : "-",
                        cell(f.getExampleValue()),
                        cell(f.getComment())));
            }
            md.append("\n");
        }

        // 枚举字典
        if (!enums.isEmpty()) {
            md.append("## 枚举字典\n\n");
            for (EnumDict e : enums) {
                md.append(String.format("### %s (%s)\n\n", text(e.getName()), text(e.getCode())));
                md.append(String.format("值类型：`%s`\n\n", text(e.getValueType())));
                if (e.getDescription() != null) {
                    md.append(e.getDescription()).append("\n\n");
                }

                List<EnumValue> values = enumDictService.listValues(e.getId());
                if (!values.isEmpty()) {
                    md.append("| 值 | 标签 | 排序 |\n");
                    md.append("|----|------|------|\n");
                    for (EnumValue v : values) {
                        md.append(String.format("| %s | %s | %d |\n",
                                cell(v.getValue()), cell(v.getLabel()), v.getSortOrder()));
                    }
                    md.append("\n");
                }
            }
        }

        // 表模板
        if (!templates.isEmpty()) {
            md.append("## 表模板\n\n");
            for (Template template : templates) {
                md.append(String.format("### %s (`%s`)\n\n",
                        text(template.getName()),
                        text(template.getTablePrefix())));
                if (template.getDescription() != null && !template.getDescription().isBlank()) {
                    md.append(template.getDescription()).append("\n\n");
                }
                List<TemplateField> templateFields = templateService.listFields(template.getId());
                if (templateFields.isEmpty()) {
                    md.append("_暂无模板字段_\n\n");
                    continue;
                }
                md.append("| 字段名 | 数据类型 | 必含 | 可空 | 默认值 | 排序 | 关联字段 | 注释 |\n");
                md.append("|--------|----------|------|------|--------|------|----------|------|\n");
                for (TemplateField field : templateFields) {
                    md.append(String.format("| %s | %s | %s | %s | %s | %s | %s | %s |\n",
                            cell(field.getName()),
                            cell(field.getDataType()),
                            boolText(field.getIsRequired()),
                            boolText(field.getNullable()),
                            cell(field.getDefaultValue()),
                            field.getSortOrder() != null ? field.getSortOrder().toString() : "-",
                            field.getFieldId() != null ? field.getFieldId().toString() : "-",
                            cell(field.getComment())));
                }
                md.append("\n");
            }
        }

        return md.toString();
    }

    private void appendOverview(StringBuilder md,
                                List<Domain> domains,
                                List<Field> fields,
                                List<EnumDict> enums,
                                List<Template> templates) {
        md.append("## 概览\n\n");
        md.append("| 项目 | 数量 |\n");
        md.append("|------|------|\n");
        md.append(String.format("| 数据域 | %d |\n", domains.size()));
        md.append(String.format("| 标准字段 | %d |\n", fields.size()));
        md.append(String.format("| 枚举字典 | %d |\n", enums.size()));
        md.append(String.format("| 表模板 | %d |\n", templates.size()));
        md.append("\n");
    }

    private Map<Long, Domain> domainsById(List<Domain> domains) {
        Map<Long, Domain> result = new LinkedHashMap<>();
        for (Domain domain : domains) {
            if (domain.getId() != null) {
                result.put(domain.getId(), domain);
            }
        }
        return result;
    }

    private String domainLabel(Long domainId, Map<Long, Domain> domainsById) {
        if (domainId == null) {
            return "-";
        }
        Domain domain = domainsById.get(domainId);
        if (domain == null) {
            return "-";
        }
        return text(domain.getName()) + "(" + text(domain.getCode()) + ")";
    }

    private String cell(String value) {
        return text(value)
                .replace("\\", "\\\\")
                .replace("|", "\\|")
                .replace("\r", " ")
                .replace("\n", " ");
    }

    private String text(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String boolText(Boolean value) {
        if (value == null) {
            return "-";
        }
        return Boolean.TRUE.equals(value) ? "是" : "否";
    }
}
