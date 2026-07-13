package com.dataspec.querynormalization.service;

import com.dataspec.querynormalization.model.QueryNormalizationResult;

/**
 * 统一执行项目级查询命名解析，供字段搜索、推荐和 Standard Query 共享。
 */
public interface QueryNormalizationService {

    /**
     * 对查询执行一次词法拆分和当前项目 glossary 解析。
     *
     * @param projectId 当前项目 ID；实现会在读取项目 glossary 前校验访问权限
     * @param query     用户查询；证据输出前会脱敏和限长
     * @return 可复用的确定性归一化结果
     */
    QueryNormalizationResult normalize(Long projectId, String query);
}
