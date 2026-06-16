package com.dataspec.template.service;

import com.dataspec.template.entity.Template;
import com.dataspec.template.entity.TemplateField;

import java.util.List;

/**
 * 表模板服务接口
 */
public interface TemplateService {
    List<Template> listByProject(Long projectId);
    Template getById(Long id);
    Template create(Template template);
    Template update(Long id, Template template);
    void delete(Long id);

    List<TemplateField> listFields(Long templateId);
    TemplateField createField(TemplateField field);
    TemplateField updateField(Long id, TemplateField field);
    void deleteField(Long id);
}
