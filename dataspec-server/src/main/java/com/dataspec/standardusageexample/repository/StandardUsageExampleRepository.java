package com.dataspec.standardusageexample.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dataspec.standardusageexample.entity.StandardUsageExample;
import com.dataspec.standardusageexample.mapper.StandardUsageExampleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 标准使用示例 Repository。查询默认依赖 MyBatis Plus 逻辑删除过滤。
 */
@Repository
@RequiredArgsConstructor
public class StandardUsageExampleRepository {

    private final StandardUsageExampleMapper standardUsageExampleMapper;

    public Optional<StandardUsageExample> findById(Long id) {
        return Optional.ofNullable(standardUsageExampleMapper.selectById(id));
    }

    public IPage<StandardUsageExample> page(Long projectId,
                                            String scope,
                                            String exampleType,
                                            String status,
                                            String query,
                                            int current,
                                            int size) {
        LambdaQueryWrapper<StandardUsageExample> wrapper = baseProjectWrapper(projectId)
                .orderByDesc(StandardUsageExample::getPriority)
                .orderByDesc(StandardUsageExample::getUpdatedAt)
                .orderByDesc(StandardUsageExample::getId);
        if (!isBlank(scope)) {
            wrapper.eq(StandardUsageExample::getScope, scope);
        }
        if (!isBlank(exampleType)) {
            wrapper.eq(StandardUsageExample::getExampleType, exampleType);
        }
        if (!isBlank(status)) {
            wrapper.eq(StandardUsageExample::getStatus, status);
        }
        if (!isBlank(query)) {
            String pattern = query.trim();
            wrapper.and(q -> q
                    .like(StandardUsageExample::getInput, pattern)
                    .or()
                    .like(StandardUsageExample::getExpectedOutput, pattern)
                    .or()
                    .like(StandardUsageExample::getAntiPattern, pattern)
                    .or()
                    .like(StandardUsageExample::getReason, pattern)
                    .or()
                    .like(StandardUsageExample::getTags, pattern)
                    .or()
                    .like(StandardUsageExample::getRuleCode, pattern));
        }
        return standardUsageExampleMapper.selectPage(new Page<>(current, size), wrapper);
    }

    public List<StandardUsageExample> findForAiContext(Long projectId, List<Long> fieldIds, String query, int limit) {
        LambdaQueryWrapper<StandardUsageExample> wrapper = baseProjectWrapper(projectId)
                .eq(StandardUsageExample::getStatus, "enabled")
                .orderByDesc(StandardUsageExample::getPriority)
                .orderByDesc(StandardUsageExample::getUpdatedAt)
                .orderByDesc(StandardUsageExample::getId)
                .last("LIMIT " + Math.max(limit, 1));
        boolean hasFields = fieldIds != null && !fieldIds.isEmpty();
        boolean hasQuery = !isBlank(query);
        if (hasFields && hasQuery) {
            String pattern = query.trim();
            wrapper.and(q -> q
                    .nested(field -> field
                            .eq(StandardUsageExample::getScope, "FIELD")
                            .in(StandardUsageExample::getFieldId, fieldIds))
                    .or(nonField -> nonField
                            .in(StandardUsageExample::getScope, List.of("GENERAL", "RULE", "TEMPLATE"))
                            .and(text -> applyUsageQuery(text, pattern))));
        } else if (hasFields) {
            wrapper.and(q -> q
                    .nested(field -> field
                            .eq(StandardUsageExample::getScope, "FIELD")
                            .in(StandardUsageExample::getFieldId, fieldIds))
                    .or()
                    .eq(StandardUsageExample::getScope, "GENERAL"));
        } else if (hasQuery) {
            String pattern = query.trim();
            wrapper.and(q -> applyUsageQuery(q, pattern));
        }
        return standardUsageExampleMapper.selectList(wrapper);
    }

    public int insert(StandardUsageExample example) {
        return standardUsageExampleMapper.insert(example);
    }

    public int update(StandardUsageExample example) {
        return standardUsageExampleMapper.updateById(example);
    }

    public int deleteById(Long id) {
        return standardUsageExampleMapper.deleteById(id);
    }

    private LambdaQueryWrapper<StandardUsageExample> baseProjectWrapper(Long projectId) {
        return new LambdaQueryWrapper<StandardUsageExample>()
                .eq(StandardUsageExample::getProjectId, projectId);
    }

    private void applyUsageQuery(LambdaQueryWrapper<StandardUsageExample> wrapper, String pattern) {
        wrapper.like(StandardUsageExample::getInput, pattern)
                .or()
                .like(StandardUsageExample::getExpectedOutput, pattern)
                .or()
                .like(StandardUsageExample::getAntiPattern, pattern)
                .or()
                .like(StandardUsageExample::getReason, pattern)
                .or()
                .like(StandardUsageExample::getTags, pattern)
                .or()
                .like(StandardUsageExample::getRuleCode, pattern);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
