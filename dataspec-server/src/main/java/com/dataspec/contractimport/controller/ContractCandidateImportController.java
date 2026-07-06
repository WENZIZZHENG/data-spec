package com.dataspec.contractimport.controller;

import com.dataspec.common.result.R;
import com.dataspec.contractimport.model.ContractCandidatePreviewPackage;
import com.dataspec.contractimport.model.ContractCandidatePreviewReq;
import com.dataspec.contractimport.service.ContractCandidateImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 契约候选导入 API，提供只读预览入口供 CLI、AI 和人工审核复用。
 */
@RestController
@RequestMapping("/api/contract-import")
@RequiredArgsConstructor
public class ContractCandidateImportController {

    private final ContractCandidateImportService contractCandidateImportService;

    /**
     * 预览契约字段候选，不创建或修改任何项目记录。
     */
    @PostMapping("/preview")
    public R<ContractCandidatePreviewPackage> preview(@Valid @RequestBody ContractCandidatePreviewReq req) {
        return R.ok(contractCandidateImportService.preview(req));
    }
}
