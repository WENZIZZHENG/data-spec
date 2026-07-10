package com.dataspec.standardref.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 标准引用解析请求。
 *
 * @param projectId 当前解析所在项目；stableRef 中的 projectId 必须与该值一致，否则按跨项目引用处理。
 * @param refType 引用类型；决定 refs 中的文本按字段、枚举、规则还是快照语义解析。
 * @param refs 待解析引用列表，可包含 stableRef、当前名称、别名、历史名或版本号；返回前会统一脱敏。
 */
@Schema(description = "标准引用解析请求；只读解析 refs，不修改标准、业务文件或数据库。")
public record StandardReferenceResolveRequest(
        @NotNull(message = "projectId 不能为空")
        @Schema(description = "当前项目 ID；所有解析都限制在该项目内，跨项目 stableRef 不会暴露目标对象信息。", example = "1")
        Long projectId,

        @NotNull(message = "refType 不能为空")
        @Schema(description = "待解析的标准对象类型。")
        StandardReferenceType refType,

        @NotEmpty(message = "refs 不能为空")
        @ArraySchema(schema = @Schema(description = "待解析引用，支持 stableRef、当前名、别名或版本号；输出会做 secret 脱敏。"))
        List<String> refs
) {
}
