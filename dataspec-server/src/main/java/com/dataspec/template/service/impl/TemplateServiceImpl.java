package com.dataspec.template.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.template.entity.Template;
import com.dataspec.template.entity.TemplateField;
import com.dataspec.template.repository.TemplateRepository;
import com.dataspec.template.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {

    private final TemplateRepository templateRepository;

    @Override
    public List<Template> listByProject(Long projectId) {
        return templateRepository.findByProjectId(projectId);
    }

    @Override
    public Template getById(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new BizException("表模板不存在: " + id));
    }

    @Override
    public Template create(Template template) {
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
        getById(id);
        templateRepository.deleteFieldsByTemplateId(id);
        templateRepository.deleteById(id);
    }

    @Override
    public List<TemplateField> listFields(Long templateId) {
        return templateRepository.findFieldsByTemplateId(templateId);
    }

    @Override
    public TemplateField createField(TemplateField field) {
        templateRepository.insertField(field);
        return field;
    }

    @Override
    public TemplateField updateField(Long id, TemplateField field) {
        field.setId(id);
        templateRepository.updateField(field);
        return field;
    }

    @Override
    public void deleteField(Long id) {
        templateRepository.deleteFieldById(id);
    }
}
