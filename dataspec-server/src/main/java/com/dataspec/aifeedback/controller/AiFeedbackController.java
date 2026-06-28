package com.dataspec.aifeedback.controller;

import com.dataspec.aifeedback.model.AiFeedbackReport;
import com.dataspec.aifeedback.service.AiFeedbackService;
import com.dataspec.common.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 使用反馈与标准改进闭环 API。
 */
@RestController
@RequestMapping("/api/ai-feedback")
@RequiredArgsConstructor
public class AiFeedbackController {

    private final AiFeedbackService aiFeedbackService;

    @GetMapping("/report")
    public R<AiFeedbackReport> report(@RequestParam Long projectId) {
        return R.ok(aiFeedbackService.buildReport(projectId));
    }
}
