package com.dataspec.fieldconflict.controller;

import com.dataspec.common.result.R;
import com.dataspec.fieldconflict.model.FieldConflictReport;
import com.dataspec.fieldconflict.service.FieldConflictService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fields/conflicts")
@RequiredArgsConstructor
public class FieldConflictController {

    private final FieldConflictService fieldConflictService;

    @GetMapping
    public R<FieldConflictReport> report(@RequestParam Long projectId) {
        return R.ok(fieldConflictService.report(projectId));
    }
}
