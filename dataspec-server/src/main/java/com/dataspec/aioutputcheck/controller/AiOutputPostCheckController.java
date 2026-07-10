package com.dataspec.aioutputcheck.controller;

import com.dataspec.aioutputcheck.model.AiOutputPostCheckRequest;
import com.dataspec.aioutputcheck.model.AiOutputPostCheckResult;
import com.dataspec.aioutputcheck.service.AiOutputPostCheckService;
import com.dataspec.common.result.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 输出后置校验 API。
 *
 * <p>该入口供前端、CLI 和 MCP 在复制、下载或执行 AI 产物前调用；控制器本身不做业务写入。</p>
 */
@RestController
@RequestMapping("/api/ai-output")
@RequiredArgsConstructor
public class AiOutputPostCheckController {

    private final AiOutputPostCheckService aiOutputPostCheckService;

    /**
     * 只读校验 AI 输出。
     *
     * @param request AI 输出文本、内容类型和项目边界。
     * @return 确定性后置校验结果。
     */
    @PostMapping("/check")
    public R<AiOutputPostCheckResult> check(@Valid @RequestBody AiOutputPostCheckRequest request) {
        return R.ok(aiOutputPostCheckService.check(request));
    }
}
