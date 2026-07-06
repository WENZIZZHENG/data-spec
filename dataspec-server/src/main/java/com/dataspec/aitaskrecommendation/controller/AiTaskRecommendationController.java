package com.dataspec.aitaskrecommendation.controller;

import com.dataspec.aitaskrecommendation.model.AiTaskRecommendationReport;
import com.dataspec.aitaskrecommendation.service.AiTaskRecommendationService;
import com.dataspec.common.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 任务推荐队列只读 API。
 */
@RestController
@RequestMapping("/api/ai-task-recommendations")
@RequiredArgsConstructor
public class AiTaskRecommendationController {

    private final AiTaskRecommendationService aiTaskRecommendationService;

    /**
     * 查询项目下一步推荐任务队列。
     *
     * @param projectId 项目 ID，服务层会校验访问边界。
     * @return AI 可消费的推荐任务队列。
     */
    @GetMapping
    public R<AiTaskRecommendationReport> report(@RequestParam Long projectId) {
        return R.ok(aiTaskRecommendationService.report(projectId));
    }
}
