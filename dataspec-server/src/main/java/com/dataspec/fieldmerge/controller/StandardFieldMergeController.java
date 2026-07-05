package com.dataspec.fieldmerge.controller;

import com.dataspec.common.result.R;
import com.dataspec.fieldmerge.model.StandardFieldMergeApplyReq;
import com.dataspec.fieldmerge.model.StandardFieldMergePreview;
import com.dataspec.fieldmerge.model.StandardFieldMergePreviewReq;
import com.dataspec.fieldmerge.model.StandardFieldMergeResult;
import com.dataspec.fieldmerge.service.StandardFieldMergeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 标准字段合并向导 API。
 */
@RestController
@RequestMapping("/api/fields/merge")
@RequiredArgsConstructor
public class StandardFieldMergeController {

    private final StandardFieldMergeService mergeService;

    /**
     * 预览字段合并影响，不写入数据。
     */
    @PostMapping("/preview")
    public R<StandardFieldMergePreview> preview(@Valid @RequestBody StandardFieldMergePreviewReq req) {
        return R.ok(mergeService.preview(req));
    }

    /**
     * 确认并应用字段合并计划。
     */
    @PostMapping("/apply")
    public R<StandardFieldMergeResult> apply(@Valid @RequestBody StandardFieldMergeApplyReq req) {
        return R.ok(mergeService.apply(req));
    }
}
