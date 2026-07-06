package com.dataspec.contractimport.service;

import com.dataspec.contractimport.model.ContractCandidatePreviewPackage;
import com.dataspec.contractimport.model.ContractCandidatePreviewReq;

/**
 * 契约候选导入预览服务，只解析契约并返回候选草案，不写入标准或候选库。
 */
public interface ContractCandidateImportService {

    /**
     * 生成契约字段候选预览包。
     *
     * @param req 只读解析请求。
     * @return 脱敏、确定性、可供人工审核的候选预览包。
     */
    ContractCandidatePreviewPackage preview(ContractCandidatePreviewReq req);
}
