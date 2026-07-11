package com.dataspec.tablemodel.service;

import com.dataspec.common.exception.BizException;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 表结构标准 JSON 编解码工具，集中处理受控结构、敏感信息拒绝和兼容空值。
 */
public class TableStructureJsonCodec {

    private final ObjectMapper objectMapper;

    public TableStructureJsonCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 将 API 结构化对象序列化为数据库 JSON 文本；发现 secret-like key/value 时直接拒绝。
     */
    public String write(Object value, String label) {
        if (value == null) {
            return null;
        }
        try {
            JsonNode node = objectMapper.valueToTree(value);
            if (node.isNull() || node.isMissingNode()) {
                return null;
            }
            if (SensitiveDataSanitizer.containsSensitiveKeyOrValue(node)) {
                throw new BizException(label + "不能包含 token、密码、Authorization、JDBC URL、DSN 或连接串");
            }
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException ex) {
            throw new BizException(label + "不是有效 JSON 结构");
        }
    }

    /**
     * 从数据库 JSON 文本读取结构化对象；历史空值或损坏值按业务异常暴露，避免静默误导 AI。
     */
    public <T> T read(String json, Class<T> valueType, T fallback, String label) {
        if (json == null || json.isBlank()) {
            return fallback;
        }
        try {
            return objectMapper.readValue(json, valueType);
        } catch (JsonProcessingException ex) {
            throw new BizException(label + "存储 JSON 无法解析");
        }
    }

    /**
     * 从数据库 JSON 文本读取列表结构。
     */
    public <T> java.util.List<T> readList(String json, Class<T> itemType, String label) {
        if (json == null || json.isBlank()) {
            return java.util.List.of();
        }
        try {
            JavaType type = objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, itemType);
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new BizException(label + "存储 JSON 无法解析");
        }
    }
}
