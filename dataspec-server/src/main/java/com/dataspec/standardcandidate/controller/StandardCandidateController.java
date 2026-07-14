package com.dataspec.standardcandidate.controller;

import com.dataspec.common.result.PageResult;
import com.dataspec.common.result.R;
import com.dataspec.standardcandidate.entity.StandardCandidate;
import com.dataspec.standardcandidate.model.StandardCandidateCreateReq;
import com.dataspec.standardcandidate.model.StandardCandidateDecisionReq;
import com.dataspec.standardcandidate.model.StandardCandidateMergeReq;
import com.dataspec.standardcandidate.model.TokenEvidenceCandidateApplyReq;
import com.dataspec.standardcandidate.model.TokenEvidenceCandidateApplyResult;
import com.dataspec.standardcandidate.model.TokenEvidenceCandidatePreview;
import com.dataspec.standardcandidate.model.TokenEvidenceCandidatePreviewReq;
import com.dataspec.standardcandidate.service.StandardCandidateService;
import com.dataspec.standardcandidate.service.TokenEvidenceCandidateService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 标准候选 Inbox API。
 */
@RestController
@RequestMapping("/api/standard-candidates")
@RequiredArgsConstructor
public class StandardCandidateController {

    private final StandardCandidateService standardCandidateService;
    private final TokenEvidenceCandidateService tokenEvidenceCandidateService;

    @GetMapping
    public R<PageResult<StandardCandidate>> page(
            @RequestParam Long projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(standardCandidateService.page(projectId, status, sourceType, keyword, current, size));
    }

    @PostMapping
    public R<StandardCandidate> create(@Valid @RequestBody StandardCandidateCreateReq req) {
        return R.ok(standardCandidateService.create(req));
    }

    /** 只读预览命名证据候选，不写入 Inbox。 */
    @Operation(summary = "预览命名证据候选", description = "把未知词、歧义缩写和禁用词整理为只读候选 signals，并签发确认 token。")
    @PostMapping("/token-evidence/preview")
    public R<TokenEvidenceCandidatePreview> previewTokenEvidence(
            @Valid @RequestBody TokenEvidenceCandidatePreviewReq req
    ) {
        return R.ok(tokenEvidenceCandidateService.preview(req));
    }

    /** 使用匹配的 dry-run token 和显式确认幂等写入 PENDING 候选。 */
    @Operation(summary = "确认命名证据候选", description = "重新校验 evidence 后幂等写入 PENDING 候选，不自动采纳或修改标准字段。")
    @PostMapping("/token-evidence/apply")
    public R<TokenEvidenceCandidateApplyResult> applyTokenEvidence(
            @Valid @RequestBody TokenEvidenceCandidateApplyReq req
    ) {
        return R.ok(tokenEvidenceCandidateService.apply(req));
    }

    @PostMapping("/{id}/accept")
    public R<StandardCandidate> accept(@PathVariable Long id, @RequestBody(required = false) StandardCandidateDecisionReq req) {
        return R.ok(standardCandidateService.accept(id, req));
    }

    @PostMapping("/{id}/merge")
    public R<StandardCandidate> merge(@PathVariable Long id, @Valid @RequestBody StandardCandidateMergeReq req) {
        return R.ok(standardCandidateService.merge(id, req));
    }

    @PostMapping("/{id}/ignore")
    public R<StandardCandidate> ignore(@PathVariable Long id, @RequestBody(required = false) StandardCandidateDecisionReq req) {
        return R.ok(standardCandidateService.ignore(id, req));
    }

    @PostMapping("/{id}/postpone")
    public R<StandardCandidate> postpone(@PathVariable Long id, @RequestBody(required = false) StandardCandidateDecisionReq req) {
        return R.ok(standardCandidateService.postpone(id, req));
    }
}
