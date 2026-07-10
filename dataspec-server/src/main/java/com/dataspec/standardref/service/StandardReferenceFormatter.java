package com.dataspec.standardref.service;

import com.dataspec.standardref.model.StandardReferenceType;

import java.util.Locale;
import java.util.Optional;

/**
 * 标准对象 stableRef 的确定性格式化与解析工具。
 *
 * <p>stableRef 第一版只承诺项目内稳定：它由对象当前 projectId 与对象自身稳定 ID/编码派生，
 * 不新增全局 ID，也不跨项目推断对象身份。</p>
 */
public final class StandardReferenceFormatter {

    private StandardReferenceFormatter() {
    }

    public static String fieldRef(Long projectId, Long fieldId) {
        return "field:" + projectId + ":" + fieldId;
    }

    public static String enumRef(Long projectId, Long codeSetId) {
        return "enum:" + projectId + ":" + codeSetId;
    }

    public static String ruleRef(Long projectId, String ruleCode) {
        return "rule:" + projectId + ":" + ruleCode;
    }

    public static String snapshotRef(Long projectId, Object snapshotIdOrVersion) {
        return "snapshot:" + projectId + ":" + snapshotIdOrVersion;
    }

    public static Optional<ParsedStableReference> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String[] parts = value.trim().split(":", 3);
        if (parts.length != 3) {
            return Optional.empty();
        }
        StandardReferenceType type = switch (parts[0].toLowerCase(Locale.ROOT)) {
            case "field" -> StandardReferenceType.FIELD;
            case "enum" -> StandardReferenceType.ENUM;
            case "rule" -> StandardReferenceType.RULE;
            case "snapshot" -> StandardReferenceType.SNAPSHOT;
            default -> null;
        };
        if (type == null) {
            return Optional.empty();
        }
        try {
            Long projectId = Long.valueOf(parts[1]);
            if (projectId <= 0 || parts[2].isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new ParsedStableReference(type, projectId, parts[2]));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    /**
     * stableRef 解析结果。
     *
     * @param type 引用对象类型。
     * @param projectId stableRef 内声明的项目 ID。
     * @param objectKey 对象 ID、规则码或快照版本片段。
     */
    public record ParsedStableReference(StandardReferenceType type, Long projectId, String objectKey) {
    }
}
