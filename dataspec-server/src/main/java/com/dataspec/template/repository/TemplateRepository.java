package com.dataspec.template.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dataspec.template.entity.Template;
import com.dataspec.template.entity.TemplateField;
import com.dataspec.template.mapper.TemplateFieldMapper;
import com.dataspec.template.mapper.TemplateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TemplateRepository {

    private final TemplateMapper templateMapper;
    private final TemplateFieldMapper templateFieldMapper;

    // ---- Template ----

    public Optional<Template> findById(Long id) {
        return Optional.ofNullable(templateMapper.selectById(id));
    }

    public List<Template> findByProjectId(Long projectId) {
        return templateMapper.selectList(
                new LambdaQueryWrapper<Template>()
                        .eq(Template::getProjectId, projectId)
                        .orderByAsc(Template::getName));
    }

    public int insert(Template template) {
        return templateMapper.insert(template);
    }

    public int update(Template template) {
        return templateMapper.updateById(template);
    }

    public int deleteById(Long id) {
        return templateMapper.deleteById(id);
    }

    // ---- TemplateField ----

    public List<TemplateField> findFieldsByTemplateId(Long templateId) {
        return templateFieldMapper.selectList(
                new LambdaQueryWrapper<TemplateField>()
                        .eq(TemplateField::getTemplateId, templateId)
                        .orderByAsc(TemplateField::getSortOrder));
    }

    public int insertField(TemplateField field) {
        return templateFieldMapper.insert(field);
    }

    public int updateField(TemplateField field) {
        return templateFieldMapper.updateById(field);
    }

    public int deleteFieldById(Long id) {
        return templateFieldMapper.deleteById(id);
    }

    public int deleteFieldsByTemplateId(Long templateId) {
        return templateFieldMapper.delete(
                new LambdaQueryWrapper<TemplateField>()
                        .eq(TemplateField::getTemplateId, templateId));
    }
}
