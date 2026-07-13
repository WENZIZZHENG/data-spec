package com.dataspec.evidenceclaim.service;

import com.dataspec.evidence.model.EvidenceSourceType;
import com.dataspec.evidenceclaim.model.EvidenceClaimResolution;

/**
 * 验证 AI Evidence claim 是否指向当前项目的持久化来源。
 */
public interface EvidenceClaimResolver {

    /**
     * 解析单个 evidence ref；调用方必须先完成当前项目访问校验。
     *
     * @param projectId 当前项目 ID
     * @param evidenceRef 待验证引用
     * @return 不泄露跨项目来源元数据的解析结果
     */
    EvidenceClaimResolution resolve(Long projectId, String evidenceRef);

    /**
     * 为支持的持久化 Evidence Package 来源生成 canonical ref。
     *
     * @return canonical ref；payload-only 或参数无效时为空
     */
    String canonicalRef(EvidenceSourceType sourceType, Long sourceId);
}
