package com.dataspec.evidence.controller;

import com.dataspec.common.result.R;
import com.dataspec.evidence.model.AiEvidencePackage;
import com.dataspec.evidence.model.AiEvidencePackageReq;
import com.dataspec.evidence.service.AiEvidencePackageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/evidence-packages")
@RequiredArgsConstructor
public class AiEvidencePackageController {

    private final AiEvidencePackageService aiEvidencePackageService;

    @PostMapping
    public R<AiEvidencePackage> generate(@Valid @RequestBody AiEvidencePackageReq req) {
        return R.ok(aiEvidencePackageService.generate(req));
    }

    @PostMapping("/download")
    public ResponseEntity<byte[]> download(@Valid @RequestBody AiEvidencePackageReq req) {
        byte[] body = aiEvidencePackageService.generateZip(req);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"dataspec-ai-evidence.zip\"")
                .body(body);
    }
}
