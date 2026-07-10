package com.dataspec.standardref.controller;

import com.dataspec.common.result.R;
import com.dataspec.standardref.model.StandardReferenceResolveRequest;
import com.dataspec.standardref.model.StandardReferenceResolveResponse;
import com.dataspec.standardref.service.StandardReferenceResolutionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 标准对象稳定引用 API。
 *
 * <p>该控制器只提供 project-scoped read-only 解析入口，供前端、CLI、MCP 和 AI agent 在采纳
 * 字段、枚举、规则或快照引用前确认其当前性、替代关系和安全诊断。</p>
 */
@RestController
@RequestMapping("/api/standard-references")
@RequiredArgsConstructor
public class StandardReferenceController {

    private final StandardReferenceResolutionService standardReferenceResolutionService;

    /**
     * 解析标准对象引用，不写入任何项目状态。
     *
     * @param request 待解析引用请求，包含项目、对象类型和引用列表。
     * @return 每条引用的稳定解析结果。
     */
    @PostMapping("/resolve")
    public R<StandardReferenceResolveResponse> resolve(@Valid @RequestBody StandardReferenceResolveRequest request) {
        return R.ok(standardReferenceResolutionService.resolve(request));
    }
}
