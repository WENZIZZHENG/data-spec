package com.dataspec.querynormalization.model;

import com.dataspec.businessglossary.model.GlossaryMatch;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 单次项目查询的共享命名归一化结果。
 *
 * @param normalizedText 经脱敏并按词法顺序连接的规范文本
 * @param lexicalTokens  有界的纯词法 token
 * @param queryTokens    可返回给 API、CLI、MCP 和前端的有界解析证据
 * @param glossaryMatches 当前项目 glossary 的确定性匹配；仅供服务端评分使用
 * @param tokenResolutions 有界 evidence 与完整 glossary 来源的内部精确关联；仅供服务端解释使用
 */
public record QueryNormalizationResult(
        String normalizedText,
        List<NameLexicalToken> lexicalTokens,
        List<QueryTokenEvidence> queryTokens,
        List<GlossaryMatch> glossaryMatches,
        List<QueryTokenResolution> tokenResolutions
) {

    /**
     * 返回保持词法顺序的去重规范 token，供字段评分复用。
     */
    public Set<String> normalizedTokenSet() {
        Set<String> tokens = new LinkedHashSet<>();
        for (NameLexicalToken token : lexicalTokens) {
            tokens.add(token.normalized());
        }
        return java.util.Collections.unmodifiableSet(tokens);
    }
}
