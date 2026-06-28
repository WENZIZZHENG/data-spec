package com.dataspec.starterkit.controller;

import com.dataspec.common.result.R;
import com.dataspec.starterkit.model.StarterKitApplyReq;
import com.dataspec.starterkit.model.StarterKitApplyResult;
import com.dataspec.starterkit.model.StarterKitDefinition;
import com.dataspec.starterkit.model.StarterKitInstallationInfo;
import com.dataspec.starterkit.service.StarterKitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/starter-kits")
@RequiredArgsConstructor
public class StarterKitController {

    private final StarterKitService starterKitService;

    @GetMapping
    public R<List<StarterKitDefinition>> listKits() {
        return R.ok(starterKitService.listKits());
    }

    @PostMapping("/apply")
    public R<StarterKitApplyResult> apply(@Valid @RequestBody StarterKitApplyReq req) {
        return R.ok(starterKitService.applyKit(req.projectId(), req.kitKey(), req.kitVersion()));
    }

    @GetMapping("/installations")
    public R<List<StarterKitInstallationInfo>> listInstallations(@RequestParam Long projectId) {
        return R.ok(starterKitService.listInstallations(projectId));
    }
}
