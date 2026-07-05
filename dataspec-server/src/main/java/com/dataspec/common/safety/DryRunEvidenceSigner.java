package com.dataspec.common.safety;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

/**
 * 为高风险写入的 dry-run evidence 生成进程内签名 token。
 *
 * <p>token 仅证明本次服务进程签发过对应预览摘要，不承载原始业务明细或 secret；服务重启后调用方需要重新
 * preview 获取新的 token，避免把可伪造的客户端字段当作安全门禁。</p>
 */
public final class DryRunEvidenceSigner {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final byte[] PROCESS_SECRET = processSecret();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private DryRunEvidenceSigner() {
    }

    /**
     * 签发只包含摘要字段的 dry-run evidence token。
     *
     * @param prefix token 前缀，用于区分业务操作。
     * @param payload 已脱敏、可放入 token 的摘要字段。
     * @param mapper JSON 序列化器；调用方应保证 payload 字段顺序稳定。
     * @return 形如 {@code prefix.payload.signature} 的签名 token。
     */
    public static String signPayload(String prefix, Map<String, Object> payload, ObjectMapper mapper) {
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("dry-run token prefix 不能为空");
        }
        try {
            String encodedPayload = URL_ENCODER.encodeToString(mapper.writeValueAsBytes(payload));
            String signingInput = prefix + "." + encodedPayload;
            String signature = URL_ENCODER.encodeToString(hmac(signingInput.getBytes(StandardCharsets.UTF_8)));
            return signingInput + "." + signature;
        } catch (Exception e) {
            throw new IllegalStateException("签发 dry-run evidence 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证 token 签名并读取摘要字段。
     *
     * @param prefix 预期 token 前缀。
     * @param token 调用方带回的 dry-run evidence token。
     * @param mapper JSON 解析器。
     * @return 签名有效时返回 payload；格式或签名错误时返回空。
     */
    public static Optional<JsonNode> verifyPayload(String prefix, String token, ObjectMapper mapper) {
        if (prefix == null || prefix.isBlank() || token == null || token.isBlank()) {
            return Optional.empty();
        }
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3 || !prefix.equals(parts[0])) {
            return Optional.empty();
        }
        try {
            String signingInput = parts[0] + "." + parts[1];
            byte[] expected = hmac(signingInput.getBytes(StandardCharsets.UTF_8));
            byte[] actual = URL_DECODER.decode(parts[2]);
            if (!MessageDigest.isEqual(expected, actual)) {
                return Optional.empty();
            }
            return Optional.of(mapper.readTree(URL_DECODER.decode(parts[1])));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /** 使用常量时间比较两个 token，避免在确认写入路径暴露签名差异。 */
    public static boolean matches(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
    }

    /** 对候选 evidence 摘要做 SHA-256，避免把字段注释或默认值直接放进 token payload。 */
    public static String sha256Hex(String value) {
        return sha256Hex(value.getBytes(StandardCharsets.UTF_8));
    }

    /** 对序列化后的 evidence 摘要做 SHA-256。 */
    public static String sha256Hex(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (Exception e) {
            throw new IllegalStateException("计算 dry-run evidence hash 失败: " + e.getMessage(), e);
        }
    }

    private static byte[] hmac(byte[] value) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(PROCESS_SECRET, HMAC_ALGORITHM));
        return mac.doFinal(value);
    }

    private static byte[] processSecret() {
        byte[] secret = new byte[32];
        new SecureRandom().nextBytes(secret);
        return secret;
    }
}
