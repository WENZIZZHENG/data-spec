package com.dataspec.standardcandidate.service.impl;

import com.dataspec.businessglossary.model.GlossaryMatch;
import com.dataspec.common.exception.BizException;
import com.dataspec.common.repository.ProjectFieldNameReservationRepository;
import com.dataspec.common.safety.DryRunEvidenceSigner;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.field.repository.FieldRepository;
import com.dataspec.querynormalization.model.QueryNormalizationResult;
import com.dataspec.querynormalization.model.QueryTokenEvidence;
import com.dataspec.querynormalization.model.QueryTokenKind;
import com.dataspec.querynormalization.model.QueryTokenResolution;
import com.dataspec.querynormalization.model.QueryTokenResolutionStatus;
import com.dataspec.querynormalization.service.QueryNormalizationService;
import com.dataspec.security.context.ProjectAccessGuard;
import com.dataspec.standardcandidate.entity.StandardCandidate;
import com.dataspec.standardcandidate.model.TokenEvidenceCandidateApplyReq;
import com.dataspec.standardcandidate.model.TokenEvidenceCandidateApplyResult;
import com.dataspec.standardcandidate.model.TokenEvidenceCandidatePayload;
import com.dataspec.standardcandidate.model.TokenEvidenceCandidatePreview;
import com.dataspec.standardcandidate.model.TokenEvidenceCandidatePreviewReq;
import com.dataspec.standardcandidate.model.TokenEvidenceCandidatePreviewStatus;
import com.dataspec.standardcandidate.model.TokenEvidenceCandidateSafety;
import com.dataspec.standardcandidate.model.TokenEvidenceCandidateSignal;
import com.dataspec.standardcandidate.model.TokenEvidenceCandidateSignalType;
import com.dataspec.standardcandidate.model.TokenEvidenceCandidateView;
import com.dataspec.standardcandidate.repository.TokenEvidenceCandidateRepository;
import com.dataspec.standardcandidate.service.TokenEvidenceCandidateService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 命名证据候选 preview/apply 实现。
 *
 * <p>该服务只创建 PENDING 候选。accept、merge、ignore、postpone 继续由既有候选服务处理，避免 AI evidence
 * 绕过人工决策边界。</p>
 */
@Service
@RequiredArgsConstructor
public class TokenEvidenceCandidateServiceImpl implements TokenEvidenceCandidateService {

    private static final String KIND_PREVIEW = "dataspec.token-evidence-candidate-preview";
    private static final String KIND_APPLY_RESULT = "dataspec.token-evidence-candidate-apply-result";
    private static final String SOURCE_TYPE = "TOKEN_EVIDENCE";
    private static final String STATUS_PENDING = "PENDING";
    private static final String TOKEN_PREFIX = "tec";
    private static final int SCHEMA_VERSION = 1;
    private static final int FIELD_NAME_MAX_LENGTH = 100;
    private static final int DISPLAY_NAME_MAX_LENGTH = 100;
    private static final int DATA_TYPE_MAX_LENGTH = 50;
    private static final int COMMENT_MAX_LENGTH = 1000;
    private static final int SOURCE_REF_MAX_LENGTH = 300;
    private static final int SOURCE_TEXT_MAX_LENGTH = 512;
    private static final Pattern FIELD_NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9_]*$");

    private final TokenEvidenceCandidateRepository candidateRepository;
    private final FieldRepository fieldRepository;
    private final ProjectFieldNameReservationRepository fieldNameReservationRepository;
    private final QueryNormalizationService queryNormalizationService;
    private final ObjectMapper objectMapper;

    @Override
    public TokenEvidenceCandidatePreview preview(TokenEvidenceCandidatePreviewReq req) {
        CandidateAnalysis analysis = analyze(req);
        PreviewConflict conflict = previewConflict(analysis);
        TokenEvidenceCandidatePreviewStatus status = conflict.status();
        String dryRunToken = status == TokenEvidenceCandidatePreviewStatus.READY
                ? signDryRun(analysis)
                : null;
        return new TokenEvidenceCandidatePreview(
                KIND_PREVIEW,
                SCHEMA_VERSION,
                analysis.projectId(),
                analysis.candidateName(),
                SOURCE_TYPE,
                analysis.sourceRef(),
                status,
                false,
                conflict.candidateId(),
                analysis.inboxPayload(),
                analysis.signals(),
                dryRunToken,
                new TokenEvidenceCandidateSafety(true, false, true, false, false),
                nextActions(status));
    }

    @Override
    @Transactional
    public TokenEvidenceCandidateApplyResult apply(TokenEvidenceCandidateApplyReq req) {
        if (req == null || !Boolean.TRUE.equals(req.confirmed())) {
            throw new BizException("写入命名证据候选前必须显式确认");
        }
        CandidateAnalysis analysis = analyze(req.previewInput());
        verifyDryRunToken(req.dryRunToken(), analysis);
        fieldNameReservationRepository.lock(analysis.projectId(), analysis.candidateName());

        Optional<StandardCandidate> existing = candidateRepository.findByFactKey(
                analysis.projectId(),
                analysis.candidateName(),
                SOURCE_TYPE,
                analysis.sourceRef());
        if (existing.isPresent()) {
            return applyResult(existing.get(), false, true);
        }

        PreviewConflict conflict = previewConflict(analysis);
        if (conflict.status() != TokenEvidenceCandidatePreviewStatus.READY) {
            throw new BizException(conflictMessage(conflict.status()));
        }

        StandardCandidate candidate = toEntity(analysis.inboxPayload());
        int inserted = candidateRepository.insertIfAbsent(candidate);
        StandardCandidate persisted = candidateRepository.findByFactKey(
                        analysis.projectId(),
                        analysis.candidateName(),
                        SOURCE_TYPE,
                        analysis.sourceRef())
                .orElseThrow(() -> new BizException("命名证据候选写入未完成，请重新预览后重试"));
        return applyResult(persisted, inserted == 1, inserted == 0);
    }

    private CandidateAnalysis analyze(TokenEvidenceCandidatePreviewReq req) {
        if (req == null || req.projectId() == null) {
            throw new BizException("项目ID不能为空");
        }
        ProjectAccessGuard.requireProjectAccess(req.projectId());
        String candidateName = required(req.candidateName(), FIELD_NAME_MAX_LENGTH, "候选字段名");
        if (!FIELD_NAME_PATTERN.matcher(candidateName).matches()) {
            throw new BizException("候选字段名必须是 snake_case");
        }
        String displayName = optional(req.displayName(), DISPLAY_NAME_MAX_LENGTH, "候选显示名");
        String dataType = required(req.dataType(), DATA_TYPE_MAX_LENGTH, "候选字段类型");
        String comment = optional(req.comment(), COMMENT_MAX_LENGTH, "候选说明");
        String sourceRef = required(req.sourceRef(), SOURCE_REF_MAX_LENGTH, "候选来源引用");
        String rawSourceText = isBlank(req.sourceText())
                ? candidateName + (isBlank(displayName) ? "" : " " + displayName)
                : req.sourceText();
        String sourceText = SensitiveDataSanitizer.redactText(rawSourceText, SOURCE_TEXT_MAX_LENGTH);
        QueryNormalizationResult normalization = queryNormalizationService.normalize(req.projectId(), sourceText);
        List<TokenEvidenceCandidateSignal> signals = actionableSignals(normalization);
        String evidenceJson = evidenceJson(sourceText, signals);
        TokenEvidenceCandidatePayload inboxPayload = new TokenEvidenceCandidatePayload(
                req.projectId(),
                candidateName,
                displayName,
                dataType,
                comment,
                SOURCE_TYPE,
                sourceRef,
                evidenceJson,
                confidence(signals));
        String dedupeHash = DryRunEvidenceSigner.sha256Hex(String.join("\u0000",
                String.valueOf(req.projectId()), candidateName, SOURCE_TYPE, sourceRef));
        String evidenceHash = DryRunEvidenceSigner.sha256Hex(evidenceJson);
        String inputHash = inputHash(inboxPayload);
        return new CandidateAnalysis(
                req.projectId(),
                candidateName,
                sourceRef,
                inboxPayload,
                signals,
                dedupeHash,
                evidenceHash,
                inputHash);
    }

    private List<TokenEvidenceCandidateSignal> actionableSignals(QueryNormalizationResult normalization) {
        Map<String, TokenEvidenceCandidateSignal> signals = new LinkedHashMap<>();
        for (QueryTokenResolution resolution : normalization.tokenResolutions()) {
            QueryTokenEvidence evidence = resolution.evidence();
            TokenEvidenceCandidateSignalType signalType = signalType(resolution);
            if (signalType == null || isRedactionPlaceholder(evidence.normalizedToken())) {
                continue;
            }
            String key = signalType + "\u0000" + evidence.normalizedToken() + "\u0000" + evidence.glossaryIds();
            signals.putIfAbsent(key, new TokenEvidenceCandidateSignal(signalType, evidence));
        }
        return List.copyOf(signals.values());
    }

    private TokenEvidenceCandidateSignalType signalType(QueryTokenResolution resolution) {
        QueryTokenEvidence evidence = resolution.evidence();
        if (evidence.resolutionStatus() == QueryTokenResolutionStatus.DISABLED) {
            return TokenEvidenceCandidateSignalType.DISABLED_NAMING;
        }
        if (evidence.resolutionStatus() == QueryTokenResolutionStatus.AMBIGUOUS
                && resolution.glossaryMatches().stream().anyMatch(this::isAbbreviation)) {
            return TokenEvidenceCandidateSignalType.AMBIGUOUS_ABBREVIATION;
        }
        if (evidence.resolutionStatus() == QueryTokenResolutionStatus.UNRESOLVED
                && evidence.tokenKind() != QueryTokenKind.NUMBER
                && evidence.tokenKind() != QueryTokenKind.UNIT) {
            return TokenEvidenceCandidateSignalType.UNKNOWN_TERM;
        }
        return null;
    }

    private boolean isAbbreviation(GlossaryMatch match) {
        return match != null && "ABBREVIATION".equals(match.matchType());
    }

    private boolean isRedactionPlaceholder(String token) {
        return token != null && token.equalsIgnoreCase("redacted");
    }

    private PreviewConflict previewConflict(CandidateAnalysis analysis) {
        Optional<StandardCandidate> exact = candidateRepository.findByFactKey(
                analysis.projectId(), analysis.candidateName(), SOURCE_TYPE, analysis.sourceRef());
        if (exact.isPresent()) {
            return new PreviewConflict(TokenEvidenceCandidatePreviewStatus.EXACT_DUPLICATE, exact.get().getId());
        }
        if (fieldRepository.existsByNameInProject(analysis.candidateName(), analysis.projectId())) {
            return new PreviewConflict(TokenEvidenceCandidatePreviewStatus.STANDARD_EXISTS, null);
        }
        Optional<StandardCandidate> nameConflict = candidateRepository.findActiveByName(
                analysis.projectId(), analysis.candidateName());
        if (nameConflict.isPresent()) {
            return new PreviewConflict(TokenEvidenceCandidatePreviewStatus.NAME_CONFLICT, nameConflict.get().getId());
        }
        if (analysis.signals().isEmpty()) {
            return new PreviewConflict(TokenEvidenceCandidatePreviewStatus.NO_ACTIONABLE_SIGNAL, null);
        }
        return new PreviewConflict(TokenEvidenceCandidatePreviewStatus.READY, null);
    }

    private String evidenceJson(String sourceText, List<TokenEvidenceCandidateSignal> signals) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("kind", "dataspec.token-evidence-candidate");
        evidence.put("schemaVersion", SCHEMA_VERSION);
        evidence.put("sourceTextHash", DryRunEvidenceSigner.sha256Hex(sourceText));
        evidence.put("signals", signals);
        try {
            return objectMapper.writeValueAsString(evidence);
        } catch (Exception error) {
            throw new IllegalStateException("序列化命名证据候选失败", error);
        }
    }

    private String signDryRun(CandidateAnalysis analysis) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("schemaVersion", SCHEMA_VERSION);
        payload.put("projectId", analysis.projectId());
        payload.put("dedupeHash", analysis.dedupeHash());
        payload.put("evidenceHash", analysis.evidenceHash());
        payload.put("inputHash", analysis.inputHash());
        return DryRunEvidenceSigner.signPayload(TOKEN_PREFIX, payload, objectMapper);
    }

    private void verifyDryRunToken(String token, CandidateAnalysis analysis) {
        JsonNode payload = DryRunEvidenceSigner.verifyPayload(TOKEN_PREFIX, token, objectMapper)
                .orElseThrow(() -> new BizException("dryRunToken 无效或已过期，请重新预览"));
        boolean matches = payload.path("schemaVersion").asInt(-1) == SCHEMA_VERSION
                && payload.path("projectId").asLong(-1) == analysis.projectId()
                && DryRunEvidenceSigner.matches(payload.path("dedupeHash").asText(), analysis.dedupeHash())
                && DryRunEvidenceSigner.matches(payload.path("evidenceHash").asText(), analysis.evidenceHash())
                && DryRunEvidenceSigner.matches(payload.path("inputHash").asText(), analysis.inputHash());
        if (!matches) {
            throw new BizException("候选输入或 token evidence 已变化，请重新预览");
        }
    }

    private String inputHash(TokenEvidenceCandidatePayload payload) {
        return DryRunEvidenceSigner.sha256Hex(String.join("\u0000",
                String.valueOf(payload.projectId()),
                payload.candidateName(),
                nullToEmpty(payload.displayName()),
                payload.dataType(),
                nullToEmpty(payload.comment()),
                payload.sourceType(),
                payload.sourceRef(),
                payload.evidenceJson(),
                String.valueOf(payload.confidence())));
    }

    private StandardCandidate toEntity(TokenEvidenceCandidatePayload payload) {
        StandardCandidate candidate = new StandardCandidate();
        candidate.setProjectId(payload.projectId());
        candidate.setCandidateName(payload.candidateName());
        candidate.setDisplayName(payload.displayName());
        candidate.setDataType(payload.dataType());
        candidate.setComment(payload.comment());
        candidate.setSourceType(SOURCE_TYPE);
        candidate.setSourceRef(payload.sourceRef());
        candidate.setEvidenceJson(payload.evidenceJson());
        candidate.setConfidence(payload.confidence());
        candidate.setStatus(STATUS_PENDING);
        return candidate;
    }

    private TokenEvidenceCandidateApplyResult applyResult(
            StandardCandidate candidate,
            boolean created,
            boolean deduplicated
    ) {
        return new TokenEvidenceCandidateApplyResult(
                KIND_APPLY_RESULT,
                SCHEMA_VERSION,
                created,
                deduplicated,
                toView(candidate),
                List.of(
                        "在标准候选 Inbox 中复核该候选。",
                        "仅在人工确认后调用 accept、merge、ignore 或 postpone。"));
    }

    private TokenEvidenceCandidateView toView(StandardCandidate candidate) {
        return new TokenEvidenceCandidateView(
                candidate.getId(),
                candidate.getProjectId(),
                candidate.getCandidateName(),
                candidate.getDisplayName(),
                candidate.getDataType(),
                candidate.getComment(),
                candidate.getSourceType(),
                candidate.getSourceRef(),
                candidate.getEvidenceJson(),
                candidate.getConfidence(),
                candidate.getStatus(),
                candidate.getTargetFieldId(),
                candidate.getDecisionReason(),
                candidate.getDecidedAt(),
                candidate.getCreatedAt(),
                candidate.getUpdatedAt());
    }

    private List<String> nextActions(TokenEvidenceCandidatePreviewStatus status) {
        return switch (status) {
            case READY -> List.of("核对候选元数据和 token signals。", "确认后带 dryRunToken 调用 apply。");
            case NO_ACTIONABLE_SIGNAL -> List.of("继续使用已解析的标准字段或补充更明确的来源文本。");
            case STANDARD_EXISTS -> List.of("使用已有标准字段，不要创建同名候选。");
            case EXACT_DUPLICATE -> List.of("打开既有 TOKEN_EVIDENCE 候选继续决策。");
            case NAME_CONFLICT -> List.of("打开同名 active 候选，合并证据或完成既有决策。");
        };
    }

    private String conflictMessage(TokenEvidenceCandidatePreviewStatus status) {
        return switch (status) {
            case STANDARD_EXISTS -> "项目内标准字段已存在，请使用已有字段";
            case EXACT_DUPLICATE -> "同一命名证据候选已存在";
            case NAME_CONFLICT -> "同名 active 候选已存在，请先处理既有候选";
            case NO_ACTIONABLE_SIGNAL -> "当前命名没有可入箱的未知词、歧义缩写或禁用词";
            case READY -> "";
        };
    }

    private int confidence(List<TokenEvidenceCandidateSignal> signals) {
        int confidence = 40;
        for (TokenEvidenceCandidateSignal signal : signals) {
            if (signal.signalType() == TokenEvidenceCandidateSignalType.DISABLED_NAMING) {
                confidence = Math.min(confidence, 10);
            } else if (signal.signalType() == TokenEvidenceCandidateSignalType.AMBIGUOUS_ABBREVIATION) {
                confidence = Math.min(confidence, 20);
            }
        }
        return confidence;
    }

    private String required(String value, int maxLength, String label) {
        if (isBlank(value)) {
            throw new BizException(label + "不能为空");
        }
        String normalized = value.trim();
        if (normalized.codePointCount(0, normalized.length()) > maxLength) {
            throw new BizException(label + "长度不能超过" + maxLength);
        }
        return SensitiveDataSanitizer.redactText(normalized, maxLength);
    }

    private String optional(String value, int maxLength, String label) {
        if (isBlank(value)) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.codePointCount(0, normalized.length()) > maxLength) {
            throw new BizException(label + "长度不能超过" + maxLength);
        }
        return SensitiveDataSanitizer.redactText(normalized, maxLength);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record CandidateAnalysis(
            Long projectId,
            String candidateName,
            String sourceRef,
            TokenEvidenceCandidatePayload inboxPayload,
            List<TokenEvidenceCandidateSignal> signals,
            String dedupeHash,
            String evidenceHash,
            String inputHash
    ) {
    }

    private record PreviewConflict(TokenEvidenceCandidatePreviewStatus status, Long candidateId) {
    }
}
