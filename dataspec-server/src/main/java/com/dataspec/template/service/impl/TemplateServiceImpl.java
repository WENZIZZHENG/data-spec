package com.dataspec.template.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.template.entity.Template;
import com.dataspec.template.entity.TemplateField;
import com.dataspec.template.repository.TemplateRepository;
import com.dataspec.security.context.ProjectAccessGuard;
import com.dataspec.template.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 表模板服务实现
 */

@Service
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {

    private final TemplateRepository templateRepository;

    @Override
    public List<Template> listByProject(Long projectId) {
        ProjectAccessGuard.requireProjectAccess(projectId);
        return templateRepository.findByProjectId(projectId);
    }

    @Override
    public Template getById(Long id) {
        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new BizException("表模板不存在: " + id));
        ProjectAccessGuard.requireProjectAccess(template.getProjectId());
        return template;
    }

    @Override
    public Template create(Template template) {
        ProjectAccessGuard.requireProjectAccess(template.getProjectId());
        templateRepository.insert(template);
        return template;
    }

    @Override
    public Template update(Long id, Template template) {
        Template existing = getById(id);
        existing.setName(template.getName());
        existing.setDescription(template.getDescription());
        existing.setTablePrefix(template.getTablePrefix());
        templateRepository.update(existing);
        return existing;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Template existing = getById(id);
        templateRepository.deleteFieldsByTemplateId(id);
        templateRepository.deleteById(id);
    }

    @Override
    public List<TemplateField> listFields(Long templateId) {
        getById(templateId);
        return templateRepository.findFieldsByTemplateId(templateId);
    }

    @Override
    public TemplateField createField(TemplateField field) {
        getById(field.getTemplateId());
        templateRepository.insertField(field);
        return field;
    }

    @Override
    public TemplateField updateField(Long id, TemplateField field) {
        TemplateField existing = getFieldById(id);
        getById(existing.getTemplateId());
        field.setId(id);
        field.setTemplateId(existing.getTemplateId());
        templateRepository.updateField(field);
        return field;
    }

    @Override
    public void deleteField(Long id) {
        TemplateField existing = getFieldById(id);
        getById(existing.getTemplateId());
        templateRepository.deleteFieldById(id);
    }

    private TemplateField getFieldById(Long id) {
        return templateRepository.findFieldById(id)
                .orElseThrow(() -> new BizException("模板字段不存在: " + id));
    }
}
