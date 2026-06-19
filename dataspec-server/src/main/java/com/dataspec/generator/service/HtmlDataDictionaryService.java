package com.dataspec.generator.service;

import com.dataspec.domain.entity.Domain;
import com.dataspec.domain.service.DomainService;
import com.dataspec.enumdict.entity.EnumDict;
import com.dataspec.enumdict.entity.EnumValue;
import com.dataspec.enumdict.service.EnumDictService;
import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.template.entity.Template;
import com.dataspec.template.entity.TemplateField;
import com.dataspec.template.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * HTML/ERD 数据字典生成器。输出必须可离线打开，因此不依赖外部脚本或样式。
 */
@Service
@RequiredArgsConstructor
public class HtmlDataDictionaryService {

    private final FieldService fieldService;
    private final DomainService domainService;
    private final EnumDictService enumDictService;
    private final TemplateService templateService;

    public String generateHtml(Long projectId) {
        DictionaryData data = loadData(projectId);
        String mermaid = generateMermaid(data);
        StringBuilder html = new StringBuilder();
        html.append("""
                <!doctype html>
                <html lang="zh-CN">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>DataSpec 数据字典</title>
                  <style>
                    :root { color-scheme: light; --border:#d8dee8; --muted:#64748b; --text:#111827; --bg:#f8fafc; --accent:#2563eb; }
                    body { margin:0; font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif; color:var(--text); background:var(--bg); }
                    main { max-width:1180px; margin:0 auto; padding:28px 24px 48px; }
                    header { margin-bottom:24px; }
                    h1 { margin:0 0 8px; font-size:30px; }
                    h2 { margin:28px 0 12px; font-size:21px; }
                    h3 { margin:18px 0 8px; font-size:17px; }
                    .subtitle { color:var(--muted); }
                    .summary { display:grid; grid-template-columns:repeat(auto-fit,minmax(160px,1fr)); gap:12px; margin:18px 0; }
                    .metric { padding:14px; border:1px solid var(--border); border-radius:8px; background:#fff; }
                    .metric strong { display:block; margin-top:4px; font-size:26px; }
                    table { width:100%; border-collapse:collapse; margin:12px 0 20px; background:#fff; }
                    th, td { border:1px solid var(--border); padding:8px 10px; text-align:left; vertical-align:top; font-size:13px; }
                    th { background:#eef2f7; font-weight:600; }
                    code, pre { font-family:"Cascadia Mono",Consolas,monospace; }
                    pre { overflow:auto; padding:14px; border:1px solid var(--border); border-radius:8px; background:#0f172a; color:#e2e8f0; line-height:1.55; }
                    .empty { color:var(--muted); font-style:italic; }
                    .tag { display:inline-block; padding:1px 7px; border-radius:999px; background:#e0ecff; color:#1d4ed8; font-size:12px; }
                  </style>
                </head>
                <body>
                <main>
                  <header>
                    <h1>DataSpec 数据字典</h1>
                    <div class="subtitle">项目级字段标准、枚举字典、表模板与关系图</div>
                  </header>
                """);

        appendOverview(html, data);
        appendMermaidSection(html, mermaid);
        appendDomains(html, data.domains());
        appendFields(html, data);
        appendEnumDicts(html, data);
        appendTemplates(html, data);
        html.append("""
                </main>
                </body>
                </html>
                """);
        return html.toString();
    }

    public String generateMermaid(Long projectId) {
        return generateMermaid(loadData(projectId));
    }

    private DictionaryData loadData(Long projectId) {
        List<Domain> domains = domainService.listByProject(projectId);
        List<Field> fields = fieldService.listByProject(projectId);
        List<EnumDict> enumDicts = enumDictService.listByProject(projectId);
        List<Template> templates = templateService.listByProject(projectId);
        Map<Long, Domain> domainsById = new LinkedHashMap<>();
        Map<Long, Field> fieldsById = new LinkedHashMap<>();
        Map<Long, EnumDict> enumDictsById = new LinkedHashMap<>();
        Map<Long, List<EnumValue>> valuesByEnumId = new LinkedHashMap<>();
        Map<Long, List<TemplateField>> templateFieldsByTemplateId = new LinkedHashMap<>();

        for (Domain domain : domains) {
            if (domain.getId() != null) {
                domainsById.put(domain.getId(), domain);
            }
        }
        for (Field field : fields) {
            if (field.getId() != null) {
                fieldsById.put(field.getId(), field);
            }
        }
        for (EnumDict enumDict : enumDicts) {
            if (enumDict.getId() != null) {
                enumDictsById.put(enumDict.getId(), enumDict);
                valuesByEnumId.put(enumDict.getId(), enumDictService.listValues(enumDict.getId()));
            }
        }
        for (Template template : templates) {
            if (template.getId() != null) {
                templateFieldsByTemplateId.put(template.getId(), templateService.listFields(template.getId()));
            }
        }
        return new DictionaryData(
                domains,
                fields,
                enumDicts,
                templates,
                domainsById,
                fieldsById,
                enumDictsById,
                valuesByEnumId,
                templateFieldsByTemplateId);
    }

    private String generateMermaid(DictionaryData data) {
        StringBuilder graph = new StringBuilder("flowchart LR\n");
        Set<String> nodes = new LinkedHashSet<>();
        Set<String> edges = new LinkedHashSet<>();
        appendNode(graph, nodes, "project", "项目标准");

        for (Domain domain : data.domains()) {
            String domainId = nodeId("domain", domain.getId(), domain.getCode());
            appendNode(graph, nodes, domainId, "数据域: " + label(domain.getName(), domain.getCode()));
            appendEdge(graph, edges, "project", domainId, "包含");
        }
        for (EnumDict enumDict : data.enumDicts()) {
            String enumId = nodeId("enum", enumDict.getId(), enumDict.getCode());
            appendNode(graph, nodes, enumId, "代码集: " + label(enumDict.getName(), enumDict.getCode()));
            appendEdge(graph, edges, "project", enumId, "定义");
        }
        for (Field field : data.fields()) {
            String fieldId = nodeId("field", field.getId(), field.getName());
            appendNode(graph, nodes, fieldId, "字段: " + text(field.getName()));
            appendEdge(graph, edges, "project", fieldId, "字段");
            if (field.getDomainId() != null && data.domainsById().containsKey(field.getDomainId())) {
                appendEdge(graph, edges, nodeId("domain", field.getDomainId(), null), fieldId, "归属");
            }
            if (field.getCodeSetId() != null && data.enumDictsById().containsKey(field.getCodeSetId())) {
                appendEdge(graph, edges, fieldId, nodeId("enum", field.getCodeSetId(), null), "引用");
            }
        }
        for (Template template : data.templates()) {
            String templateId = nodeId("template", template.getId(), template.getName());
            appendNode(graph, nodes, templateId, "模板: " + label(template.getName(), template.getTablePrefix()));
            appendEdge(graph, edges, "project", templateId, "模板");
            List<TemplateField> templateFields = data.templateFieldsByTemplateId().getOrDefault(template.getId(), List.of());
            for (TemplateField templateField : templateFields) {
                String templateFieldId = nodeId("template_field",
                        templateField.getId(),
                        template.getId() + "_" + templateField.getName() + "_" + templateField.getSortOrder());
                appendNode(graph, nodes, templateFieldId, "模板字段: " + text(templateField.getName()));
                appendEdge(graph, edges, templateId, templateFieldId, "包含");
                if (templateField.getFieldId() != null && data.fieldsById().containsKey(templateField.getFieldId())) {
                    appendEdge(graph, edges, templateFieldId, nodeId("field", templateField.getFieldId(), null), "关联标准字段");
                }
            }
        }
        return graph.toString();
    }

    private void appendOverview(StringBuilder html, DictionaryData data) {
        html.append("<section><h2>概览</h2><div class=\"summary\">");
        appendMetric(html, "数据域", data.domains().size());
        appendMetric(html, "标准字段", data.fields().size());
        appendMetric(html, "枚举字典", data.enumDicts().size());
        appendMetric(html, "表模板", data.templates().size());
        html.append("</div></section>\n");
    }

    private void appendMetric(StringBuilder html, String label, int value) {
        html.append("<div class=\"metric\"><span>")
                .append(escapeHtml(label))
                .append("</span><strong>")
                .append(value)
                .append("</strong></div>");
    }

    private void appendMermaidSection(StringBuilder html, String mermaid) {
        html.append("<section><h2>关系图</h2><p class=\"subtitle\">Mermaid flowchart，可复制到支持 Mermaid 的工具中渲染。</p><pre>")
                .append(escapeHtml(mermaid))
                .append("</pre></section>\n");
    }

    private void appendDomains(StringBuilder html, List<Domain> domains) {
        html.append("<section><h2>数据域</h2>");
        if (domains.isEmpty()) {
            html.append("<p class=\"empty\">暂无数据域</p></section>\n");
            return;
        }
        html.append("<table><thead><tr><th>编码</th><th>名称</th><th>描述</th></tr></thead><tbody>");
        for (Domain domain : domains) {
            html.append("<tr><td>").append(cell(domain.getCode())).append("</td><td>")
                    .append(cell(domain.getName())).append("</td><td>")
                    .append(cell(domain.getDescription())).append("</td></tr>");
        }
        html.append("</tbody></table></section>\n");
    }

    private void appendFields(StringBuilder html, DictionaryData data) {
        html.append("<section><h2>标准字段库</h2>");
        if (data.fields().isEmpty()) {
            html.append("<p class=\"empty\">暂无标准字段</p></section>\n");
            return;
        }
        html.append("""
                <table><thead><tr>
                <th>字段名</th><th>显示名</th><th>数据域</th><th>数据类型</th><th>可空</th><th>默认值</th>
                <th>敏感</th><th>状态</th><th>代码集</th><th>别名</th><th>分类</th><th>示例</th><th>注释</th>
                </tr></thead><tbody>
                """);
        for (Field field : data.fields()) {
            html.append("<tr><td><code>").append(cell(field.getName())).append("</code></td><td>")
                    .append(cell(field.getDisplayName())).append("</td><td>")
                    .append(cell(domainLabel(field.getDomainId(), data.domainsById()))).append("</td><td><code>")
                    .append(cell(field.getDataType())).append("</code></td><td>")
                    .append(boolText(field.getNullable())).append("</td><td>")
                    .append(cell(field.getDefaultValue())).append("</td><td>")
                    .append(boolText(field.getSensitive())).append("</td><td>")
                    .append(cell(field.getStatus())).append("</td><td>")
                    .append(cell(enumLabel(field.getCodeSetId(), data.enumDictsById()))).append("</td><td>")
                    .append(cell(field.getAliases())).append("</td><td>")
                    .append(cell(field.getCategory())).append("</td><td>")
                    .append(cell(field.getExampleValue())).append("</td><td>")
                    .append(cell(field.getComment())).append("</td></tr>");
        }
        html.append("</tbody></table></section>\n");
    }

    private void appendEnumDicts(StringBuilder html, DictionaryData data) {
        html.append("<section><h2>枚举字典</h2>");
        if (data.enumDicts().isEmpty()) {
            html.append("<p class=\"empty\">暂无枚举字典</p></section>\n");
            return;
        }
        for (EnumDict enumDict : data.enumDicts()) {
            html.append("<h3>").append(escapeHtml(label(enumDict.getName(), enumDict.getCode()))).append("</h3>")
                    .append("<p class=\"subtitle\">值类型：<code>").append(cell(enumDict.getValueType()))
                    .append("</code>，").append(cell(enumDict.getDescription())).append("</p>");
            List<EnumValue> values = data.valuesByEnumId().getOrDefault(enumDict.getId(), List.of());
            if (values.isEmpty()) {
                html.append("<p class=\"empty\">暂无枚举值</p>");
                continue;
            }
            html.append("<table><thead><tr><th>值</th><th>标签</th><th>排序</th></tr></thead><tbody>");
            for (EnumValue value : values) {
                html.append("<tr><td><code>").append(cell(value.getValue())).append("</code></td><td>")
                        .append(cell(value.getLabel())).append("</td><td>")
                        .append(value.getSortOrder() == null ? "-" : value.getSortOrder()).append("</td></tr>");
            }
            html.append("</tbody></table>");
        }
        html.append("</section>\n");
    }

    private void appendTemplates(StringBuilder html, DictionaryData data) {
        html.append("<section><h2>表模板</h2>");
        if (data.templates().isEmpty()) {
            html.append("<p class=\"empty\">暂无表模板</p></section>\n");
            return;
        }
        for (Template template : data.templates()) {
            html.append("<h3>").append(escapeHtml(label(template.getName(), template.getTablePrefix()))).append("</h3>")
                    .append("<p class=\"subtitle\">").append(cell(template.getDescription())).append("</p>");
            List<TemplateField> templateFields = data.templateFieldsByTemplateId().getOrDefault(template.getId(), List.of());
            if (templateFields.isEmpty()) {
                html.append("<p class=\"empty\">暂无模板字段</p>");
                continue;
            }
            html.append("""
                    <table><thead><tr>
                    <th>字段名</th><th>数据类型</th><th>必含</th><th>可空</th><th>默认值</th><th>排序</th><th>关联标准字段</th><th>注释</th>
                    </tr></thead><tbody>
                    """);
            for (TemplateField field : templateFields) {
                html.append("<tr><td><code>").append(cell(field.getName())).append("</code></td><td><code>")
                        .append(cell(field.getDataType())).append("</code></td><td>")
                        .append(boolText(field.getIsRequired())).append("</td><td>")
                        .append(boolText(field.getNullable())).append("</td><td>")
                        .append(cell(field.getDefaultValue())).append("</td><td>")
                        .append(field.getSortOrder() == null ? "-" : field.getSortOrder()).append("</td><td>")
                        .append(cell(templateFieldLabel(field.getFieldId(), data.fieldsById()))).append("</td><td>")
                        .append(cell(field.getComment())).append("</td></tr>");
            }
            html.append("</tbody></table>");
        }
        html.append("</section>\n");
    }

    private void appendNode(StringBuilder graph, Set<String> nodes, String id, String label) {
        if (nodes.add(id)) {
            graph.append("  ").append(id).append("[\"").append(mermaidLabel(label)).append("\"]\n");
        }
    }

    private void appendEdge(StringBuilder graph, Set<String> edges, String from, String to, String label) {
        String edge = from + "--" + label + "-->" + to;
        if (edges.add(edge)) {
            graph.append("  ").append(from)
                    .append(" -- \"").append(mermaidLabel(label)).append("\" --> ")
                    .append(to).append("\n");
        }
    }

    private String nodeId(String prefix, Object key, String fallback) {
        String raw = key == null ? fallback : key.toString();
        String normalized = text(raw).replaceAll("[^A-Za-z0-9_]", "_");
        if (normalized.isBlank() || "-".equals(normalized)) {
            normalized = "item";
        }
        return prefix + "_" + normalized;
    }

    private String label(String name, String code) {
        if (isBlank(code)) {
            return text(name);
        }
        return text(name) + " (" + code + ")";
    }

    private String domainLabel(Long domainId, Map<Long, Domain> domainsById) {
        Domain domain = domainId == null ? null : domainsById.get(domainId);
        return domain == null ? "-" : label(domain.getName(), domain.getCode());
    }

    private String enumLabel(Long enumId, Map<Long, EnumDict> enumDictsById) {
        EnumDict enumDict = enumId == null ? null : enumDictsById.get(enumId);
        return enumDict == null ? "-" : label(enumDict.getName(), enumDict.getCode());
    }

    private String templateFieldLabel(Long fieldId, Map<Long, Field> fieldsById) {
        Field field = fieldId == null ? null : fieldsById.get(fieldId);
        return field == null ? "-" : text(field.getName());
    }

    private String cell(String value) {
        return escapeHtml(text(value));
    }

    private String text(Object value) {
        if (value == null) {
            return "-";
        }
        String text = value.toString();
        return text.isBlank() ? "-" : text;
    }

    private String boolText(Boolean value) {
        if (value == null) {
            return "-";
        }
        return Boolean.TRUE.equals(value) ? "是" : "否";
    }

    private String escapeHtml(String value) {
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '&' -> escaped.append("&amp;");
                case '<' -> escaped.append("&lt;");
                case '>' -> escaped.append("&gt;");
                case '"' -> escaped.append("&quot;");
                case '\'' -> escaped.append("&#39;");
                case '\r', '\n' -> escaped.append(' ');
                default -> escaped.append(ch);
            }
        }
        return escaped.toString();
    }

    private String mermaidLabel(String value) {
        return text(value)
                .replace("\"", "'")
                .replace("\r", " ")
                .replace("\n", " ");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record DictionaryData(
            List<Domain> domains,
            List<Field> fields,
            List<EnumDict> enumDicts,
            List<Template> templates,
            Map<Long, Domain> domainsById,
            Map<Long, Field> fieldsById,
            Map<Long, EnumDict> enumDictsById,
            Map<Long, List<EnumValue>> valuesByEnumId,
            Map<Long, List<TemplateField>> templateFieldsByTemplateId) {
    }
}
