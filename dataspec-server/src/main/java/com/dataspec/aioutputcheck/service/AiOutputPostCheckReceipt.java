package com.dataspec.aioutputcheck.service;

import com.dataspec.aioutputcheck.model.AiOutputPostCheckStatus;
import com.dataspec.common.safety.DryRunEvidenceSigner;
import com.dataspec.reviewfinding.model.ReviewFinding;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 签发和验证 AI output post-check 的进程内 receipt。
 *
 * <p>receipt 只绑定项目、PASS/safeToUse 状态和规范化外部 findings 的完整摘要，
 * 不包含 finding 正文或 secret。它不绑定 Evidence Package 的 sourceType/sourceId，也不提供一次性消费语义，
 * 因此同一进程内可为同项目、同 findings 重复验证；进程重启后签名密钥变化，调用方必须重新 post-check。</p>
 */
public final class AiOutputPostCheckReceipt {

    private static final String TOKEN_PREFIX = "aopcr1";
    private static final int RECEIPT_SCHEMA_VERSION = 1;

    private AiOutputPostCheckReceipt() {
    }

    /**
     * 为一次通过的 post-check 签发 receipt。
     *
     * @param projectId 当前项目 ID
     * @param status post-check 状态
     * @param safeToUse 是否允许使用产物
     * @param normalizedFindings 已由服务端规范化的外部 findings
     * @param mapper JSON 序列化器
     * @return 仅 PASS 且 safeToUse=true 时返回 receipt，否则返回 null
     */
    public static String issue(
            Long projectId,
            AiOutputPostCheckStatus status,
            boolean safeToUse,
            List<ReviewFinding> normalizedFindings,
            ObjectMapper mapper
    ) {
        if (projectId == null || status != AiOutputPostCheckStatus.PASS || !safeToUse) {
            return null;
        }
        return DryRunEvidenceSigner.signPayload(
                TOKEN_PREFIX,
                payload(projectId, status, safeToUse, normalizedFindings),
                mapper);
    }

    /**
     * 验证 receipt 是否对应当前项目和完整规范化 findings。
     *
     * @param receipt 调用方从 post-check 带回的 receipt
     * @param projectId Evidence Package 所属项目
     * @param normalizedFindings Evidence Package 重新规范化后的外部 findings
     * @param mapper JSON 解析器
     * @return 签名、状态、项目、数量和完整摘要均一致时为 true
     */
    public static boolean verify(
            String receipt,
            Long projectId,
            List<ReviewFinding> normalizedFindings,
            ObjectMapper mapper
    ) {
        if (projectId == null) {
            return false;
        }
        JsonNode signed = DryRunEvidenceSigner.verifyPayload(TOKEN_PREFIX, receipt, mapper).orElse(null);
        List<ReviewFinding> findings = normalizedFindings == null ? List.of() : normalizedFindings;
        return signed != null
                && signed.path("schemaVersion").asInt(-1) == RECEIPT_SCHEMA_VERSION
                && signed.path("projectId").canConvertToLong()
                && signed.path("projectId").asLong() == projectId
                && AiOutputPostCheckStatus.PASS.name().equals(signed.path("status").asText())
                && signed.path("safeToUse").asBoolean(false)
                && signed.path("findingCount").asInt(-1) == findings.size()
                && DryRunEvidenceSigner.matches(
                        signed.path("findingsDigest").asText(null),
                        findingsDigest(findings));
    }

    private static Map<String, Object> payload(
            Long projectId,
            AiOutputPostCheckStatus status,
            boolean safeToUse,
            List<ReviewFinding> normalizedFindings
    ) {
        List<ReviewFinding> findings = normalizedFindings == null ? List.of() : normalizedFindings;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("schemaVersion", RECEIPT_SCHEMA_VERSION);
        payload.put("projectId", projectId);
        payload.put("status", status.name());
        payload.put("safeToUse", safeToUse);
        payload.put("findingCount", findings.size());
        payload.put("findingsDigest", findingsDigest(findings));
        return payload;
    }

    private static String findingsDigest(List<ReviewFinding> findings) {
        StringBuilder canonical = new StringBuilder();
        append(canonical, findings.size());
        for (ReviewFinding finding : findings) {
            appendFinding(canonical, finding);
        }
        return DryRunEvidenceSigner.sha256Hex(canonical.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void appendFinding(StringBuilder target, ReviewFinding finding) {
        append(target, finding == null ? null : finding.schemaVersion());
        append(target, finding == null ? null : finding.source());
        append(target, finding == null ? null : finding.findingKey());
        append(target, finding == null ? null : finding.code());
        append(target, finding == null ? null : finding.severity());
        append(target, finding == null || finding.subject() == null ? null : finding.subject().projectId());
        append(target, finding == null || finding.subject() == null ? null : finding.subject().kind());
        append(target, finding == null || finding.subject() == null ? null : finding.subject().name());
        append(target, finding == null || finding.subject() == null ? null : finding.subject().tableName());
        append(target, finding == null || finding.subject() == null ? null : finding.subject().columnName());
        append(target, finding == null || finding.subject() == null ? null : finding.subject().stableRef());
        append(target, finding == null || finding.location() == null ? null : finding.location().path());
        append(target, finding == null || finding.location() == null ? null : finding.location().line());
        append(target, finding == null || finding.location() == null ? null : finding.location().column());
        append(target, finding == null || finding.location() == null ? null : finding.location().lineEnd());
        append(target, finding == null || finding.location() == null ? null : finding.location().columnEnd());
        append(target, finding == null || finding.location() == null ? null : finding.location().sourceStart());
        append(target, finding == null || finding.location() == null ? null : finding.location().sourceEnd());
        append(target, finding == null || finding.location() == null ? null : finding.location().locationKind());
        append(target, finding == null ? null : finding.trigger());
        append(target, finding == null ? null : finding.expected());
        append(target, finding == null ? null : finding.observed());
        List<String> refs = finding == null ? List.of() : finding.evidenceRefs();
        append(target, refs.size());
        refs.forEach(ref -> append(target, ref));
        append(target, finding == null ? null : finding.confidence());
        append(target, finding == null ? null : finding.suggestedFix());
        append(target, finding == null ? null : finding.autoFixSafe());
        append(target, finding == null || finding.waiver() == null ? null : finding.waiver().waived());
        append(target, finding == null || finding.waiver() == null ? null : finding.waiver().waiverId());
        append(target, finding == null || finding.waiver() == null ? null : finding.waiver().reason());
    }

    private static void append(StringBuilder target, Object value) {
        if (value == null) {
            target.append("-1:");
            return;
        }
        String text = String.valueOf(value);
        target.append(text.getBytes(StandardCharsets.UTF_8).length).append(':').append(text);
    }
}
