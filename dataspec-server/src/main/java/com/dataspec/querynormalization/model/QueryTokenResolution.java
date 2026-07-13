package com.dataspec.querynormalization.model;

import com.dataspec.businessglossary.model.GlossaryMatch;

import java.util.List;
import java.util.Objects;

/**
 * 服务端内部的 query token 解析关联，保留公共 evidence 与完整 glossary 来源之间的精确映射。
 *
 * <p>公共 evidence 会脱敏和限长，不能再用于反推来源类型；该模型仅在服务端评分与
 * Explain Trace 生成期间使用，不作为 API、CLI 或 MCP 响应字段。</p>
 *
 * @param evidence 可安全返回调用方的有界 token evidence
 * @param glossaryMatches 产生该 evidence 的完整内部 glossary 匹配；未解析时为空
 */
public record QueryTokenResolution(
        QueryTokenEvidence evidence,
        List<GlossaryMatch> glossaryMatches
) {

    public QueryTokenResolution {
        Objects.requireNonNull(evidence, "evidence");
        glossaryMatches = glossaryMatches == null ? List.of() : List.copyOf(glossaryMatches);
    }
}
