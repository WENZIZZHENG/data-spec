package com.dataspec.reverseimport.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据库 COMMENT SQL 方言支持摘要。
 */
@Schema(description = "数据库 COMMENT SQL 方言支持摘要；说明表/列注释是否能安全生成 dry-run SQL。")
@Data
public class DatabaseCommentDialectSupport {

    /** 数据库类型，如 POSTGRESQL 或 MYSQL。 */
    @Schema(description = "数据库类型，如 POSTGRESQL 或 MYSQL。")
    private String databaseType;

    /** true 表示当前方言可安全生成表 COMMENT dry-run SQL。 */
    @Schema(description = "true 表示当前方言可安全生成表 COMMENT dry-run SQL。")
    private Boolean tableCommentSqlSupported = false;

    /** true 表示当前方言可安全生成列 COMMENT dry-run SQL。 */
    @Schema(description = "true 表示当前方言可安全生成列 COMMENT dry-run SQL。")
    private Boolean columnCommentSqlSupported = false;

    /** 不支持或需人工处理的原因。 */
    @Schema(description = "不支持或需人工处理的原因；不得包含凭据或业务数据行。")
    private List<String> unsupportedReasons = new ArrayList<>();

    /** 方言相关补充说明。 */
    @Schema(description = "方言相关补充说明；用于前端和 CLI 展示人工处理边界。")
    private List<String> notes = new ArrayList<>();
}
