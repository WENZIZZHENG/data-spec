package com.dataspec.generator.service;

import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.enumdict.entity.EnumDict;
import com.dataspec.enumdict.entity.EnumValue;
import com.dataspec.enumdict.service.EnumDictService;
import com.dataspec.domain.entity.Domain;
import com.dataspec.domain.service.DomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Markdown 数据字典生成器
 */
@Service
@RequiredArgsConstructor
public class MarkdownGeneratorService {

    private final FieldService fieldService;
    private final DomainService domainService;
    private final EnumDictService enumDictService;

    /**
     * 生成项目级 Markdown 数据字典
     */
    public String generateDataDictionary(Long projectId) {
        StringBuilder md = new StringBuilder();
        md.append("# 数据字典\n\n");

        // 数据域
        List<Domain> domains = domainService.listByProject(projectId);
        if (!domains.isEmpty()) {
            md.append("## 数据域\n\n");
            md.append("| 编码 | 名称 | 描述 |\n");
            md.append("|------|------|------|\n");
            for (Domain d : domains) {
                md.append(String.format("| %s | %s | %s |\n",
                        d.getCode(), d.getName(), nullSafe(d.getDescription())));
            }
            md.append("\n");
        }

        // 标准字段
        List<Field> fields = fieldService.listByProject(projectId);
        if (!fields.isEmpty()) {
            md.append("## 标准字段库\n\n");
            md.append("| 字段名 | 显示名 | 数据类型 | 可空 | 默认值 | 注释 |\n");
            md.append("|--------|--------|----------|------|--------|------|\n");
            for (Field f : fields) {
                md.append(String.format("| %s | %s | %s | %s | %s | %s |\n",
                        f.getName(),
                        nullSafe(f.getDisplayName()),
                        f.getDataType(),
                        f.getNullable() ? "是" : "否",
                        nullSafe(f.getDefaultValue()),
                        nullSafe(f.getComment())));
            }
            md.append("\n");
        }

        // 枚举字典
        List<EnumDict> enums = enumDictService.listByProject(projectId);
        if (!enums.isEmpty()) {
            md.append("## 枚举字典\n\n");
            for (EnumDict e : enums) {
                md.append(String.format("### %s (%s)\n\n", e.getName(), e.getCode()));
                if (e.getDescription() != null) {
                    md.append(e.getDescription()).append("\n\n");
                }

                List<EnumValue> values = enumDictService.listValues(e.getId());
                if (!values.isEmpty()) {
                    md.append("| 值 | 标签 | 排序 |\n");
                    md.append("|----|------|------|\n");
                    for (EnumValue v : values) {
                        md.append(String.format("| %s | %s | %d |\n",
                                v.getValue(), v.getLabel(), v.getSortOrder()));
                    }
                    md.append("\n");
                }
            }
        }

        return md.toString();
    }

    private String nullSafe(String s) {
        return s != null ? s : "-";
    }
}
