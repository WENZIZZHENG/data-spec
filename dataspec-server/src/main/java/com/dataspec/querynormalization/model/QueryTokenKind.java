package com.dataspec.querynormalization.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 确定性词法 token 类型，用于解释名称边界而非推断业务语义。
 */
@Schema(description = "确定性词法 token 类型；仅描述命名边界，不代表业务分类。")
public enum QueryTokenKind {
    /** 普通英文或拉丁字母词。 */
    WORD,
    /** 连续大写 acronym，例如 HTTP、ID。 */
    ACRONYM,
    /** 纯数字片段。 */
    NUMBER,
    /** 有界内置单位集合中的完整 token，例如 ms、kg。 */
    UNIT,
    /** 连续汉字片段；是否可解析由项目 glossary 决定。 */
    HAN
}
