package com.dataspec.standardref.service;

import com.dataspec.standardref.model.StandardReferenceResolveRequest;
import com.dataspec.standardref.model.StandardReferenceResolveResponse;

/**
 * 项目内标准对象引用解析服务。
 *
 * <p>服务只读解析字段、枚举、规则和快照引用，不修改标准、不访问跨项目对象元数据，
 * 输出给 API/CLI/MCP/AI 的文本必须保持 secret-safe。</p>
 */
public interface StandardReferenceResolutionService {

    /**
     * 按请求顺序解析标准对象引用。
     *
     * @param request 项目、引用类型和待解析文本。
     * @return 稳定 JSON 契约响应，包含每条引用的状态、canonicalRef、替代建议和证据。
     */
    StandardReferenceResolveResponse resolve(StandardReferenceResolveRequest request);
}
