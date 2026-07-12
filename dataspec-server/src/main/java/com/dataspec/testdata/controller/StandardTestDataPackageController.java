package com.dataspec.testdata.controller;

import com.dataspec.common.result.R;
import com.dataspec.testdata.model.StandardTestDataPackage;
import com.dataspec.testdata.model.StandardTestDataPackageReq;
import com.dataspec.testdata.service.StandardTestDataPackageService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 标准测试数据包 API。
 *
 * <p>该接口只生成安全、确定性的合成 mock/seed/case 草稿，不写入项目标准、业务仓库或源数据库。</p>
 */
@RestController
@RequestMapping("/api/test-data/package")
@RequiredArgsConstructor
public class StandardTestDataPackageController {

    private final StandardTestDataPackageService standardTestDataPackageService;

    /**
     * 生成标准驱动测试数据包，结果仅作为测试、mock 和 AI 用例草稿。
     */
    @Operation(operationId = "generatePackage", summary = "生成标准驱动测试数据包")
    @PostMapping("/generate")
    public R<StandardTestDataPackage> generate(@RequestBody StandardTestDataPackageReq req) {
        return R.ok(standardTestDataPackageService.generate(req));
    }
}
