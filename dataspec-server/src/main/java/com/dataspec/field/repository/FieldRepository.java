package com.dataspec.field.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dataspec.field.entity.Field;
import com.dataspec.field.mapper.FieldMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 标准字段 Repository —— 封装字段维度的业务查询
 */

@Repository
@RequiredArgsConstructor
public class FieldRepository {

    private static final int DEFAULT_KNOWLEDGE_CARD_LIMIT = 20;
    private static final int MAX_KNOWLEDGE_CARD_LIMIT = 100;

    private final FieldMapper fieldMapper;

    /** 根据 ID 查找字段 */
    public Optional<Field> findById(Long id) {
        return Optional.ofNullable(fieldMapper.selectById(id));
    }

    /** 分页查询项目下的字段 */
    public IPage<Field> findByProjectId(Long projectId, int current, int size) {
        return fieldMapper.selectPage(
                new Page<>(current, size),
                new LambdaQueryWrapper<Field>()
                        .eq(Field::getProjectId, projectId)
                        .orderByAsc(Field::getName));
    }

    /** 查询项目下所有字段（不分页） */
    public List<Field> findAllByProjectId(Long projectId) {
        return fieldMapper.selectList(
                new LambdaQueryWrapper<Field>()
                        .eq(Field::getProjectId, projectId)
                        .orderByAsc(Field::getName));
    }

    /**
     * 在项目内按名称批量查询字段。
     *
     * <p>批量创建流程在取得项目字段名事务锁后调用该方法刷新锁前快照，避免并发事务已经创建
     * 同名字段时继续依据旧列表重复插入。</p>
     */
    public List<Field> findByNamesInProject(Collection<String> names, Long projectId) {
        List<String> normalizedNames = names == null
                ? List.of()
                : names.stream()
                        .filter(name -> name != null && !name.isBlank())
                        .distinct()
                        .toList();
        if (normalizedNames.isEmpty()) {
            return List.of();
        }
        return fieldMapper.selectList(
                new LambdaQueryWrapper<Field>()
                        .eq(Field::getProjectId, projectId)
                        .in(Field::getName, normalizedNames)
                        .orderByAsc(Field::getName));
    }

    /**
     * 查询字段知识卡候选字段，并在持久化层下推 query、status 和 limit，避免 AI/前端列表入口先拉取项目全量字段。
     */
    public List<Field> findKnowledgeCardCandidates(Long projectId, String query, String status, int limit) {
        return fieldMapper.selectList(
                knowledgeCardCandidateWrapper(projectId, query, status)
                        .orderByAsc(Field::getStatus)
                        .orderByAsc(Field::getName)
                        .last("LIMIT " + safeKnowledgeCardLimit(limit)));
    }

    /**
     * 统计字段知识卡候选数量；只返回 count，不加载字段明细。
     */
    public long countKnowledgeCardCandidates(Long projectId, String query, String status) {
        return fieldMapper.selectCount(knowledgeCardCandidateWrapper(projectId, query, status));
    }

    private LambdaQueryWrapper<Field> knowledgeCardCandidateWrapper(Long projectId, String query, String status) {
        LambdaQueryWrapper<Field> wrapper = new LambdaQueryWrapper<Field>()
                .eq(Field::getProjectId, projectId);
        if (status != null && !status.isBlank()) {
            wrapper.eq(Field::getStatus, status.trim());
        }
        if (query != null && !query.isBlank()) {
            String like = query.trim();
            wrapper.and(nested -> nested
                    .like(Field::getName, like)
                    .or()
                    .like(Field::getDisplayName, like)
                    .or()
                    .like(Field::getComment, like)
                    .or()
                    .like(Field::getTags, like)
                    .or()
                    .like(Field::getAliases, like)
                    .or()
                    .like(Field::getCategory, like)
                    .or()
                    .like(Field::getPreferredEnglishName, like)
                    .or()
                    .like(Field::getLocalizedNamesJson, like)
                    .or()
                    .like(Field::getTranslationAliasesJson, like)
                    .or()
                    .like(Field::getForbiddenTranslationsJson, like)
                    .or()
                    .like(Field::getTranslationNotes, like)
                    .or()
                    .like(Field::getSemanticSummary, like)
                    .or()
                    .like(Field::getPreferredUseCases, like)
                    .or()
                    .like(Field::getAvoidWhen, like)
                    .or()
                    .like(Field::getAggregationHints, like)
                    .or()
                    .like(Field::getReplacementGuidance, like)
                    .or()
                    .like(Field::getMisuseExamples, like));
        }
        return wrapper;
    }

    private int safeKnowledgeCardLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_KNOWLEDGE_CARD_LIMIT;
        }
        return Math.min(limit, MAX_KNOWLEDGE_CARD_LIMIT);
    }

    /** 根据数据域查找字段 */
    public List<Field> findByDomainId(Long domainId) {
        return fieldMapper.selectList(
                new LambdaQueryWrapper<Field>()
                        .eq(Field::getDomainId, domainId)
                        .orderByAsc(Field::getName));
    }

    /** 根据项目内字段名查找字段 */
    public Optional<Field> findByNameInProject(String name, Long projectId) {
        return Optional.ofNullable(fieldMapper.selectOne(
                new LambdaQueryWrapper<Field>()
                        .eq(Field::getName, name)
                        .eq(Field::getProjectId, projectId)
                        .last("limit 1")));
    }

    /** 检查项目内字段名是否重复 */
    public boolean existsByNameInProject(String name, Long projectId) {
        return fieldMapper.exists(
                new LambdaQueryWrapper<Field>()
                        .eq(Field::getName, name)
                        .eq(Field::getProjectId, projectId));
    }

    /** 检查项目内字段名是否重复（排除指定 ID） */
    public boolean existsByNameInProjectExcludeId(String name, Long projectId, Long excludeId) {
        return fieldMapper.exists(
                new LambdaQueryWrapper<Field>()
                        .eq(Field::getName, name)
                        .eq(Field::getProjectId, projectId)
                        .ne(Field::getId, excludeId));
    }

    /** 新增字段 */
    public int insert(Field field) {
        return fieldMapper.insert(field);
    }

    /** 更新字段 */
    public int update(Field field) {
        return fieldMapper.updateById(field);
    }

    /**
     * 合并来源字段时的条件废弃更新。
     * <p>
     * 该方法只在来源字段仍未设置 replacement 时生效，用于阻止两个合并请求同时把同一个来源字段合并到不同目标字段。
     */
    public int deprecateSourceForMergeIfReplacementUnset(Field source) {
        LocalDateTime now = LocalDateTime.now();
        return fieldMapper.update(null, new LambdaUpdateWrapper<Field>()
                .eq(Field::getId, source.getId())
                .eq(Field::getProjectId, source.getProjectId())
                .isNull(Field::getReplacementFieldId)
                .set(Field::getStatus, source.getStatus())
                .set(Field::getReplacementFieldId, source.getReplacementFieldId())
                .set(Field::getReplacementReason, source.getReplacementReason())
                .set(Field::getUpdatedAt, now));
    }

    /** 逻辑删除字段 */
    public int deleteById(Long id) {
        return fieldMapper.deleteById(id);
    }
}
