package com.dataspec.capability.service;

import com.dataspec.capability.model.AiCapabilityCatalog;
import com.dataspec.capability.model.AiCapabilityEntry;
import com.dataspec.capability.model.VersionCompatibilityResponse;

/**
 * 面向 AI/CLI/MCP 的能力发现与兼容握手服务。
 *
 * <p>该服务只暴露只读元数据；具体业务能力的权限、幂等和写入约束仍由各自 API 执行时校验。</p>
 */
public interface AiCapabilityCatalogService {

    /**
     * 获取 AI 能力清单。
     *
     * @param projectId 可选项目 ID；提供时会校验当前 token 的项目访问权。
     * @return 当前 DataSpec 面向 AI 的能力清单。
     */
    AiCapabilityCatalog getCatalog(Long projectId);

    /**
     * 获取单个能力条目。
     *
     * @param capabilityId 能力 ID，支持横线和下划线形式。
     * @param projectId 可选项目 ID；提供时会校验当前 token 的项目访问权。
     * @return 单个能力条目。
     */
    AiCapabilityEntry getCapability(String capabilityId, Long projectId);

    /**
     * 获取服务端与客户端之间的版本兼容握手。
     *
     * @param client 客户端类型，例如 cli 或 mcp；仅用于诊断文案，不参与授权。
     * @param clientVersion 客户端版本；缺失或无法解析时返回 UNKNOWN 而非误判不兼容。
     * @return 只读兼容握手响应，不包含凭据、连接串或业务数据。
     */
    VersionCompatibilityResponse getVersionCompatibility(String client, String clientVersion);
}
