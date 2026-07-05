package com.dataspec.fieldmerge.service;

import com.dataspec.fieldmerge.model.StandardFieldMergeApplyReq;
import com.dataspec.fieldmerge.model.StandardFieldMergePreview;
import com.dataspec.fieldmerge.model.StandardFieldMergePreviewReq;
import com.dataspec.fieldmerge.model.StandardFieldMergeResult;

/**
 * 标准字段合并用例服务。
 */
public interface StandardFieldMergeService {

    /**
     * 生成标准字段合并预览，不写入字段库或变更日志。
     */
    StandardFieldMergePreview preview(StandardFieldMergePreviewReq req);

    /**
     * 应用标准字段合并计划，写入目标字段、来源字段和标准变更日志。
     */
    StandardFieldMergeResult apply(StandardFieldMergeApplyReq req);
}
