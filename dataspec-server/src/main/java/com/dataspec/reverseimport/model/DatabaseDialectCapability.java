package com.dataspec.reverseimport.model;

import java.util.List;

/**
 * 数据库方言 metadata 能力画像。该对象只描述能力与限制，不包含连接凭据。
 */
public record DatabaseDialectCapability(
        String dialect,
        String schemaSupport,
        String commentSupport,
        String indexSupport,
        Boolean metadataReadable,
        List<String> supportedWorkflows) {

    public DatabaseDialectCapability {
        supportedWorkflows = supportedWorkflows == null ? List.of() : List.copyOf(supportedWorkflows);
    }
}
