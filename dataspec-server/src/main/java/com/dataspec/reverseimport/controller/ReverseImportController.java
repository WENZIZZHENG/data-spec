package com.dataspec.reverseimport.controller;

import com.dataspec.common.result.R;
import com.dataspec.reverseimport.model.ReverseImportPreview;
import com.dataspec.reverseimport.service.ReverseImportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SQL 反向导入 API。
 */
@RestController
@RequestMapping("/api/reverse-import")
@RequiredArgsConstructor
public class ReverseImportController {

    private final ReverseImportService reverseImportService;

    @PostMapping("/preview")
    public R<ReverseImportPreview> preview(@Valid @RequestBody ReverseImportReq req) {
        return R.ok(reverseImportService.preview(req.projectId(), req.sql()));
    }

    public record ReverseImportReq(
            @NotNull(message = "项目ID不能为空") Long projectId,
            @NotBlank(message = "SQL 不能为空") String sql
    ) {}
}
