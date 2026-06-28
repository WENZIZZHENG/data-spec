package com.dataspec.standardchange.service;

import com.dataspec.standardchange.model.FieldChangePreviewReq;
import com.dataspec.standardchange.model.RuleChangePreviewReq;
import com.dataspec.standardchange.model.StandardChangePreview;

/**
 * 标准变更保存前预览服务。
 */
public interface StandardChangePreviewService {

    StandardChangePreview previewFieldUpdate(Long fieldId, FieldChangePreviewReq req);

    StandardChangePreview previewRuleUpdate(Long ruleId, RuleChangePreviewReq req);

    StandardChangePreview previewRuleToggle(Long ruleId, Long projectId, boolean enabled);
}
