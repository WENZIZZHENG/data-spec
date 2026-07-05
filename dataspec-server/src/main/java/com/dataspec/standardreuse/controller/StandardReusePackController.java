package com.dataspec.standardreuse.controller;

import com.dataspec.common.result.R;
import com.dataspec.standardreuse.model.StandardReusePackApplicationInfo;
import com.dataspec.standardreuse.model.StandardReusePackApplyReq;
import com.dataspec.standardreuse.model.StandardReusePackApplyResult;
import com.dataspec.standardreuse.model.StandardReusePackCreateReq;
import com.dataspec.standardreuse.model.StandardReusePackDetail;
import com.dataspec.standardreuse.model.StandardReusePackDriftReport;
import com.dataspec.standardreuse.model.StandardReusePackInfo;
import com.dataspec.standardreuse.model.StandardReusePackPlan;
import com.dataspec.standardreuse.service.StandardReusePackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 标准复用包 API。
 *
 * <p>用于个人 / 小团队在项目之间复用字段、枚举、规则和模板，并查看轻量漂移报告。</p>
 */
@RestController
@RequestMapping("/api/standard-reuse-packs")
@RequiredArgsConstructor
public class StandardReusePackController {

    private final StandardReusePackService service;

    @GetMapping
    public R<List<StandardReusePackInfo>> listPacks(@RequestParam Long projectId) {
        return R.ok(service.listPacks(projectId));
    }

    @PostMapping
    public R<StandardReusePackDetail> createPack(@Valid @RequestBody StandardReusePackCreateReq req) {
        return R.ok(service.createPack(req));
    }

    @GetMapping("/{packId}")
    public R<StandardReusePackDetail> getPack(@PathVariable Long packId) {
        return R.ok(service.getPack(packId));
    }

    @PostMapping("/apply/preview")
    public R<StandardReusePackPlan> previewApply(@Valid @RequestBody StandardReusePackApplyReq req) {
        return R.ok(service.previewApply(req));
    }

    @PostMapping("/apply")
    public R<StandardReusePackApplyResult> applyPack(@Valid @RequestBody StandardReusePackApplyReq req) {
        return R.ok(service.applyPack(req));
    }

    @GetMapping("/applications")
    public R<List<StandardReusePackApplicationInfo>> listApplications(@RequestParam Long projectId) {
        return R.ok(service.listApplications(projectId));
    }

    @GetMapping("/{packId}/drift")
    public R<StandardReusePackDriftReport> driftReport(@PathVariable Long packId, @RequestParam Long projectId) {
        return R.ok(service.driftReport(packId, projectId));
    }
}
