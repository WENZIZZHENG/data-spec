package com.dataspec.standardcandidate.controller;

import com.dataspec.common.result.PageResult;
import com.dataspec.common.result.R;
import com.dataspec.standardcandidate.entity.StandardCandidate;
import com.dataspec.standardcandidate.model.StandardCandidateCreateReq;
import com.dataspec.standardcandidate.model.StandardCandidateDecisionReq;
import com.dataspec.standardcandidate.model.StandardCandidateMergeReq;
import com.dataspec.standardcandidate.service.StandardCandidateService;
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
