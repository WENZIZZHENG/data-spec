package com.dataspec.standardcandidate.service;

import com.dataspec.standardcandidate.model.TokenEvidenceCandidateApplyReq;
import com.dataspec.standardcandidate.model.TokenEvidenceCandidateApplyResult;
import com.dataspec.standardcandidate.model.TokenEvidenceCandidatePreview;
import com.dataspec.standardcandidate.model.TokenEvidenceCandidatePreviewReq;

/** 命名证据候选 preview/apply 用例边界。 */
public interface TokenEvidenceCandidateService {

    /**
     * 生成不会写入 Inbox 的命名证据候选预览。
     *
     * @param req 项目、候选元数据和稳定来源引用
     * @return 可审阅 signals、冲突状态和 READY 时的签名 token
     */
    TokenEvidenceCandidatePreview preview(TokenEvidenceCandidatePreviewReq req);

    /**
     * 在显式确认和证据未漂移时幂等写入 PENDING 候选。
     *
     * @param req 原预览输入、签名 token 和确认状态
     * @return 新建或重复命中的同一候选
     */
    TokenEvidenceCandidateApplyResult apply(TokenEvidenceCandidateApplyReq req);
}
