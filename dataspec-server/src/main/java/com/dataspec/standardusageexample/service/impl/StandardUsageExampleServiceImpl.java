package com.dataspec.standardusageexample.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dataspec.common.exception.BizException;
import com.dataspec.common.result.PageResult;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.security.context.ProjectAccessGuard;
import com.dataspec.standardusageexample.entity.StandardUsageExample;
import com.dataspec.standardusageexample.model.StandardUsageExampleSaveReq;
import com.dataspec.standardusageexample.repository.StandardUsageExampleRepository;
import com.dataspec.standardusageexample.service.StandardUsageExampleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 标准使用示例服务。示例会进入 AI Context，因此保存前必须拦截明显 secret 和真实连接串。
 */
@Service
@RequiredArgsConstructor
public class StandardUsageExampleServiceImpl implements StandardUsageExampleService {

    private static final Set<String> SCOPES = Set.of("FIELD", "RULE", "TEMPLATE", "GENERAL");
    private static final Set<String> EXAMPLE_TYPES = Set.of("GOOD", "BAD");
    private static final Set<String> STATUSES = Set.of("enabled", "disabled");
    private static final int DEFAULT_PRIORITY = 50;
    private static final int MAX_PRIORITY = 100;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_CONTEXT_EXAMPLES = 30;
    private static final int MAX_TEXT_LENGTH = 4_000;
    private static final int MAX_TAGS_LENGTH = 500;

    private final StandardUsageExampleRepository standardUsageExampleRepository;

    @Override
    public PageResult<StandardUsageExample> page(Long projectId,
                                                 String scope,
                                                 String exampleType,
                                                 String status,
                                                 String query,
                                                 int current,
                                                 int size) {
        requireProject(projectId);
        IPage<StandardUsageExample> page = standardUsageExampleRepository.page(
                projectId,
                normalizeOptional(scope, SCOPES, "scope"),
                normalizeOptional(exampleType, EXAMPLE_TYPES, "exampleType"),
                normalizeOptionalStatus(status),
                trimToNull(query),
                Math.max(current, 1),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        return PageResult.of(page);
    }

    @Override
    @Transactional
    public StandardUsageExample create(StandardUsageExampleSaveReq req) {
        validateSaveRequest(req);
        StandardUsageExample example = new StandardUsageExample();
        applyEditable(example, req);
        standardUsageExampleRepository.insert(example);
        return example;
    }

    @Override
    @Transactional
    public StandardUsageExample update(Long id, StandardUsageExampleSaveReq req) {
        if (id == null) {
            throw new BizException("示例ID不能为空");
        }
        StandardUsageExample existing = standardUsageExampleRepository.findById(id)
                .orElseThrow(() -> new BizException("示例不存在: " + id));
        requireProject(existing.getProjectId());
        if (req == null || req.getProjectId() == null) {
            throw new BizException("项目ID不能为空");
        }
        if (!existing.getProjectId().equals(req.getProjectId())) {
            throw new BizException("示例不属于当前项目");
        }
        validateSaveRequest(req);
        applyEditable(existing, req);
        existing.setProjectId(req.getProjectId());
        standardUsageExampleRepository.update(existing);
        return existing;
    }

    @Override
    @Transactional
    public void delete(Long projectId, Long id) {
        requireProject(projectId);
        if (id == null) {
            throw new BizException("示例ID不能为空");
        }
        StandardUsageExample existing = standardUsageExampleRepository.findById(id)
                .orElseThrow(() -> new BizException("示例不存在: " + id));
        if (!projectId.equals(existing.getProjectId())) {
            throw new BizException("示例不属于当前项目");
        }
        standardUsageExampleRepository.deleteById(id);
    }

    @Override
    public List<StandardUsageExample> selectForAiContext(Long projectId, List<Long> fieldIds, String query, int limit) {
        requireProject(projectId);
        int safeLimit = Math.min(Math.max(limit, 1), MAX_CONTEXT_EXAMPLES);
        return standardUsageExampleRepository.findForAiContext(projectId, fieldIds == null ? List.of() : fieldIds, trimToNull(query), safeLimit);
    }

    private void validateSaveRequest(StandardUsageExampleSaveReq req) {
        if (req == null || req.getProjectId() == null) {
            throw new BizException("项目ID不能为空");
        }
        requireProject(req.getProjectId());
        String scope = normalizeRequired(req.getScope(), SCOPES, "scope");
        String exampleType = normalizeRequired(req.getExampleType(), EXAMPLE_TYPES, "exampleType");
        normalizeOptionalStatus(req.getStatus());
        required(req.getInput(), "input 不能为空");
        required(req.getReason(), "reason 不能为空");
        if ("FIELD".equals(scope) && req.getFieldId() == null) {
            throw new BizException("FIELD 示例必须关联 fieldId");
        }
        if ("RULE".equals(scope)) {
            required(req.getRuleCode(), "RULE 示例必须关联 ruleCode");
        }
        if ("TEMPLATE".equals(scope) && req.getTemplateId() == null) {
            throw new BizException("TEMPLATE 示例必须关联 templateId");
        }
        if ("GOOD".equals(exampleType)) {
            required(req.getExpectedOutput(), "GOOD 示例必须填写 expectedOutput");
        }
        if ("BAD".equals(exampleType)) {
            required(req.getAntiPattern(), "BAD 示例必须填写 antiPattern");
        }
        rejectUnsafe(req);
    }

    private void applyEditable(StandardUsageExample target, StandardUsageExampleSaveReq req) {
        target.setProjectId(req.getProjectId());
        target.setFieldId(req.getFieldId());
        target.setRuleCode(trimToNull(req.getRuleCode()));
        target.setTemplateId(req.getTemplateId());
        target.setScope(normalizeRequired(req.getScope(), SCOPES, "scope"));
        target.setExampleType(normalizeRequired(req.getExampleType(), EXAMPLE_TYPES, "exampleType"));
        target.setInput(ensureMaxLength(required(req.getInput(), "input 不能为空"), MAX_TEXT_LENGTH, "input"));
        target.setExpectedOutput(ensureMaxLength(trimToNull(req.getExpectedOutput()), MAX_TEXT_LENGTH, "expectedOutput"));
        target.setAntiPattern(ensureMaxLength(trimToNull(req.getAntiPattern()), MAX_TEXT_LENGTH, "antiPattern"));
        target.setReason(ensureMaxLength(required(req.getReason(), "reason 不能为空"), MAX_TEXT_LENGTH, "reason"));
        target.setTags(ensureMaxLength(normalizeTags(req.getTags()), MAX_TAGS_LENGTH, "tags"));
        target.setPriority(clampPriority(req.getPriority()));
        target.setStatus(normalizeOptionalStatus(req.getStatus()));
    }

    private void rejectUnsafe(StandardUsageExampleSaveReq req) {
        List<String> values = List.of(
                nullToEmpty(req.getInput()),
                nullToEmpty(req.getExpectedOutput()),
                nullToEmpty(req.getAntiPattern()),
                nullToEmpty(req.getReason()),
                nullToEmpty(req.getTags())
        );
        for (String value : values) {
            if (SensitiveDataSanitizer.containsSensitiveText(value)) {
                throw new BizException("示例内容包含疑似敏感信息，请先脱敏后保存");
            }
        }
    }

    private void requireProject(Long projectId) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }
        ProjectAccessGuard.requireProjectAccess(projectId);
    }

    private String normalizeRequired(String value, Set<String> allowed, String fieldName) {
        String normalized = normalizeOptional(value, allowed, fieldName);
        if (normalized == null) {
            throw new BizException(fieldName + " 不能为空");
        }
        return normalized;
    }

    private String normalizeOptional(String value, Set<String> allowed, String fieldName) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw new BizException(fieldName + " 不支持: " + normalized);
        }
        return normalized;
    }

    private String normalizeOptionalStatus(String value) {
        String status = trimToNull(value);
        if (status == null) {
            return "enabled";
        }
        status = status.toLowerCase(Locale.ROOT);
        if (!STATUSES.contains(status)) {
            throw new BizException("status 不支持: " + status);
        }
        return status;
    }

    private String normalizeTags(String value) {
        String tags = trimToNull(value);
        if (tags == null) {
            return null;
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .distinct()
                .reduce((left, right) -> left + "," + right)
                .orElse(null);
    }

    private String required(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new BizException(message);
        }
        return normalized;
    }

    private String ensureMaxLength(String value, int maxLength, String fieldName) {
        if (value != null && value.length() > maxLength) {
            throw new BizException(fieldName + " 长度不能超过" + maxLength);
        }
        return value;
    }

    private int clampPriority(Integer priority) {
        if (priority == null) {
            return DEFAULT_PRIORITY;
        }
        return Math.max(0, Math.min(priority, MAX_PRIORITY));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
