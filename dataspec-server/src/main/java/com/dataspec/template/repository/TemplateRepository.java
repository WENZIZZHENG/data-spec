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

/**
 * 表模板 Repository —— 封装模板和模板字段的业务查询
 */

@Repository
@RequiredArgsConstructor
public class TemplateRepository {

    private final TemplateMapper templateMapper;
    private final TemplateFieldMapper templateFieldMapper;

    // ---- Template ----

    /** 根据 ID 查找模板 */
    public Optional<Template> findById(Long id) {
        return Optional.ofNullable(templateMapper.selectById(id));
    }

    /** 查询项目下所有模板 */
    public List<Template> findByProjectId(Long projectId) {
        return templateMapper.selectList(
                new LambdaQueryWrapper<Template>()
                        .eq(Template::getProjectId, projectId)
                        .orderByAsc(Template::getName));
    }

    /** 根据项目内模板名称查找模板 */
    public Optional<Template> findByNameInProject(String name, Long projectId) {
        return Optional.ofNullable(templateMapper.selectOne(
                new LambdaQueryWrapper<Template>()
                        .eq(Template::getName, name)
                        .eq(Template::getProjectId, projectId)
                        .last("limit 1")));
    }

    /** 新增模板 */
    public int insert(Template template) {
        return templateMapper.insert(template);
    }

    /** 更新模板 */
    public int update(Template template) {
        return templateMapper.updateById(template);
    }

    /** 逻辑删除模板 */
    public int deleteById(Long id) {
        return templateMapper.deleteById(id);
    }

    // ---- TemplateField ----

    /** 查询模板下所有字段（按排序升序） */
    public List<TemplateField> findFieldsByTemplateId(Long templateId) {
        return templateFieldMapper.selectList(
                new LambdaQueryWrapper<TemplateField>()
                        .eq(TemplateField::getTemplateId, templateId)
                        .orderByAsc(TemplateField::getSortOrder));
    }

    /** 根据 ID 查找模板字段 */
    public Optional<TemplateField> findFieldById(Long id) {
        return Optional.ofNullable(templateFieldMapper.selectById(id));
    }

    /** 新增模板字段 */
    public int insertField(TemplateField field) {
        return templateFieldMapper.insert(field);
    }

    /** 更新模板字段 */
    public int updateField(TemplateField field) {
        return templateFieldMapper.updateById(field);
    }

    /** 逻辑删除模板字段 */
    public int deleteFieldById(Long id) {
        return templateFieldMapper.deleteById(id);
    }

    /** 删除模板下所有字段 */
    public int deleteFieldsByTemplateId(Long templateId) {
        return templateFieldMapper.delete(
                new LambdaQueryWrapper<TemplateField>()
                        .eq(TemplateField::getTemplateId, templateId));
    }
}
