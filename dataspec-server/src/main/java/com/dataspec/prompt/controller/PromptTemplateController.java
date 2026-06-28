package com.dataspec.prompt.controller;

import com.dataspec.common.result.R;
import com.dataspec.prompt.model.PromptTemplateDefinition;
import com.dataspec.prompt.model.PromptTemplateEvalReq;
import com.dataspec.prompt.model.PromptTemplateEvalResult;
import com.dataspec.prompt.service.PromptTemplateEvaluationService;
import com.dataspec.prompt.service.PromptTemplateRegistry;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Prompt 模板元数据与本地评测 API。
 */
@RestController
@RequestMapping("/api/prompt-templates")
@RequiredArgsConstructor
public class PromptTemplateController {

    private final PromptTemplateRegistry registry;
    private final PromptTemplateEvaluationService evaluationService;

    @GetMapping
    public R<List<PromptTemplateDefinition>> listTemplates() {
        return R.ok(registry.listTemplates());
    }

    @PostMapping("/evaluate")
    public R<PromptTemplateEvalResult> evaluate(@Valid @RequestBody PromptTemplateEvalReq req) {
        return R.ok(evaluationService.evaluate(req.templateKey(), req.output()));
    }
}
