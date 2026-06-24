package com.dataspec.security.dto;

/**
 * API token 创建响应。
 *
 * @param plainToken 一次性明文 token；只在创建接口返回，后续不可查询
 * @param token token 元数据
 */
public record ApiTokenCreateResp(
        String plainToken,
        ApiTokenInfo token
) {
}
