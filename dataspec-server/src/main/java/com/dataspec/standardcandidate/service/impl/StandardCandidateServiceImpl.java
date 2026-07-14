package com.dataspec.standardcandidate.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dataspec.common.exception.BizException;
import com.dataspec.common.repository.ProjectFieldNameReservationRepository;
import com.dataspec.common.result.PageResult;
import com.dataspec.field.entity.Field;
import com.dataspec.field.repository.FieldRepository;
import com.dataspec.field.service.FieldService;
import com.dataspec.security.context.ProjectAccessGuard;
import com.dataspec.standardcandidate.entity.StandardCandidate;
import com.dataspec.standardcandidate.model.StandardCandidateCreateReq;
import com.dataspec.standardcandidate.model.StandardCandidateDecisionReq;
import com.dataspec.standardcandidate.model.StandardCandidateMergeReq;
import com.dataspec.standardcandidate.repository.StandardCandidateRepository;
import com.dataspec.standardcandidate.service.StandardCandidateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 标准候选 Inbox 服务。所有决策都显式记录，不自动改写已有标准字段。
 */
@Service
@RequiredArgsConstructor
public class StandardCandidateServiceImpl implements StandardCandidateService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_ACCEPTED = "ACCEPTED";
    private static final String STATUS_MERGED = "MERGED";
    private static final String STATUS_IGNORED = "IGNORED";
    private static final String STATUS_POSTPONED = "POSTPONED";
    private static final String RESERVED_TOKEN_EVIDENCE_SOURCE = "TOKEN_EVIDENCE";
    private static final Set<String> DECIDABLE_STATUSES = Set.of(STATUS_PENDING, STATUS_POSTPONED);
    private static final int DEFAULT_CONFIDENCE = 50;
    private static final int FIELD_NAME_MAX_LENGTH = 100;
    private static final int DISPLAY_NAME_MAX_LENGTH = 100;
    private static final int DATA_TYPE_MAX_LENGTH = 50;
    private static final int SOURCE_TYPE_MAX_LENGTH = 50;
    private static final int SOURCE_REF_MAX_LENGTH = 300;
    private static final Pattern JDBC_URL = Pattern.compile("jdbc:[^\\s\"'<>]+", Pattern.CASE_INSENSITIVE);
    // 候选证据可能来自文本或 JSON，脱敏需同时覆盖 password=xxx 与 "token":"xxx" 两类写法。
    private static final Pattern SECRET_ASSIGNMENT = Pattern.compile(
            "(?i)((?:\"|')?(?:password|pwd|token|api[_-]?token)(?:\"|')?\\s*[:=]\\s*)([\"']?)[^\\s\"';&,}]+\\2");
    private static final Pattern TOKEN = Pattern.compile("(?i)(bearer\\s+)[A-Za-z0-9._\\-]+");

    private final StandardCandidateRepository standardCandidateRepository;
    private final FieldRepository fieldRepository;
    private final FieldService fieldService;
    private final ProjectFieldNameReservationRepository fieldNameReservationRepository;

    @Override
    public PageResult<StandardCandidate> page(Long projectId, String status, String sourceType, String keyword, int current, int size) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }
        ProjectAccessGuard.requireProjectAccess(projectId);
        IPage<StandardCandidate> page = standardCandidateRepository.page(
                projectId,
                normalizeOptionalCode(status),
                normalizeOptionalCode(sourceType),
                keyword,
                Math.max(current, 1),
                Math.min(Math.max(size, 1), 100));
        return PageResult.of(page);
    }

    @Override
    @Transactional
    public StandardCandidate create(StandardCandidateCreateReq req) {
        if (req == null || req.projectId() == null) {
            throw new BizException("项目ID不能为空");
        }
        ProjectAccessGuard.requireProjectAccess(req.projectId());
        String name = ensureMaxLength(required(req.candidateName(), "候选字段名不能为空"), FIELD_NAME_MAX_LENGTH, "候选字段名");
        String sourceType = ensureMaxLength(
                required(req.sourceType(), "候选来源不能为空").toUpperCase(Locale.ROOT),
                SOURCE_TYPE_MAX_LENGTH,
                "候选来源");
        if (RESERVED_TOKEN_EVIDENCE_SOURCE.equals(sourceType)) {
            throw new BizException("TOKEN_EVIDENCE 是受控来源，请使用命名证据 preview/apply 接口");
        }
        // 字段和候选共享同一命名空间，锁必须先于两个表的冲突检查。
        fieldNameReservationRepository.lock(req.projectId(), name);
        if (fieldRepository.existsByNameInProject(name, req.projectId())) {
            throw new BizException("标准字段已存在，请合并到已有字段: " + name);
        }
        if (standardCandidateRepository.existsActiveByNameInProject(req.projectId(), name)) {
            throw new BizException("候选字段已存在，请先处理现有候选: " + name);
        }
        StandardCandidate candidate = new StandardCandidate();
        candidate.setProjectId(req.projectId());
        candidate.setCandidateName(name);
        candidate.setDisplayName(ensureMaxLength(sanitize(req.displayName()), DISPLAY_NAME_MAX_LENGTH, "候选显示名"));
        candidate.setDataType(ensureMaxLength(required(req.dataType(), "候选字段类型不能为空"), DATA_TYPE_MAX_LENGTH, "候选字段类型"));
        candidate.setComment(sanitize(req.comment()));
        candidate.setSourceType(sourceType);
        candidate.setSourceRef(ensureMaxLength(sanitize(req.sourceRef()), SOURCE_REF_MAX_LENGTH, "候选来源引用"));
        candidate.setEvidenceJson(sanitize(req.evidenceJson()));
        candidate.setConfidence(clampConfidence(req.confidence()));
        candidate.setStatus(STATUS_PENDING);
        standardCandidateRepository.insert(candidate);
        return candidate;
    }

    @Override
    @Transactional
    public StandardCandidate accept(Long id, StandardCandidateDecisionReq req) {
        StandardCandidate candidate = getForDecision(id);
        Field field = new Field();
        field.setProjectId(candidate.getProjectId());
        field.setName(candidate.getCandidateName());
        field.setDisplayName(candidate.getDisplayName());
        field.setDataType(candidate.getDataType());
        field.setComment(candidate.getComment());
        field.setNullable(true);
        field.setStatus("enabled");
        Field created = fieldService.createFromCandidate(field, candidate.getId());
        return decide(candidate, STATUS_ACCEPTED, created.getId(), reason(req));
    }

    @Override
    @Transactional
    public StandardCandidate merge(Long id, StandardCandidateMergeReq req) {
        if (req == null || req.targetFieldId() == null) {
            throw new BizException("目标字段ID不能为空");
        }
        StandardCandidate candidate = getForDecision(id);
        Field target = fieldService.getById(req.targetFieldId());
        if (!candidate.getProjectId().equals(target.getProjectId())) {
            throw new BizException("目标字段不属于候选所在项目");
        }
        return decide(candidate, STATUS_MERGED, target.getId(), sanitize(req.reason()));
    }

    @Override
    @Transactional
    public StandardCandidate ignore(Long id, StandardCandidateDecisionReq req) {
        StandardCandidate candidate = getForDecision(id);
        return decide(candidate, STATUS_IGNORED, null, reason(req));
    }

    @Override
    @Transactional
    public StandardCandidate postpone(Long id, StandardCandidateDecisionReq req) {
        StandardCandidate candidate = getForDecision(id);
        return decide(candidate, STATUS_POSTPONED, null, reason(req));
    }

    private StandardCandidate getForDecision(Long id) {
        StandardCandidate candidate = standardCandidateRepository.findById(id)
                .orElseThrow(() -> new BizException("标准候选不存在: " + id));
        ProjectAccessGuard.requireProjectAccess(candidate.getProjectId());
        if (!DECIDABLE_STATUSES.contains(candidate.getStatus())) {
            throw new BizException("当前候选状态不可决策: " + candidate.getStatus());
        }
        return candidate;
    }

    private StandardCandidate decide(StandardCandidate candidate, String status, Long targetFieldId, String reason) {
        candidate.setStatus(status);
        candidate.setTargetFieldId(targetFieldId);
        candidate.setDecisionReason(sanitize(reason));
        candidate.setDecidedAt(LocalDateTime.now());
        standardCandidateRepository.update(candidate);
        return candidate;
    }

    private String reason(StandardCandidateDecisionReq req) {
        return req == null ? null : sanitize(req.reason());
    }

    private Integer clampConfidence(Integer confidence) {
        if (confidence == null) {
            return DEFAULT_CONFIDENCE;
        }
        return Math.max(0, Math.min(confidence, 100));
    }

    private String normalizeOptionalCode(String value) {
        return isBlank(value) ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String required(String value, String message) {
        if (isBlank(value)) {
            throw new BizException(message);
        }
        return value.trim();
    }

    private String ensureMaxLength(String value, int maxLength, String label) {
        if (value != null && value.length() > maxLength) {
            throw new BizException(label + "长度不能超过" + maxLength);
        }
        return value;
    }

    private String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String sanitized = JDBC_URL.matcher(value).replaceAll("jdbc:***");
        sanitized = SECRET_ASSIGNMENT.matcher(sanitized).replaceAll("$1$2***$2");
        sanitized = TOKEN.matcher(sanitized).replaceAll("$1***");
        return sanitized;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
