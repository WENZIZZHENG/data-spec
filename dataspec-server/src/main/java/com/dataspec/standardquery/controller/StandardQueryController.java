package com.dataspec.standardquery.controller;

import com.dataspec.common.result.ErrorDetail;
import com.dataspec.common.result.R;
import com.dataspec.standardquery.exception.StandardQueryValidationException;
import com.dataspec.standardquery.model.StandardQueryRequest;
import com.dataspec.standardquery.model.StandardQueryResult;
import com.dataspec.standardquery.model.StandardQueryValidationError;
import com.dataspec.standardquery.service.StandardQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Standard Query DSL 只读查询 API。
 *
 * <p>该接口只执行项目内标准对象元数据查询，不写入标准、业务文件、数据库或 AI Context 缓存。</p>
 */
@RestController
@RequestMapping("/api/standard-query")
@RequiredArgsConstructor
public class StandardQueryController {

    private final StandardQueryService standardQueryService;

    /**
     * 执行 Standard Query DSL 查询。
     *
     * @param request DSL 请求；所有 query/filter 值均按敏感输入处理。
     * @return 脱敏、可解释、有界的查询结果。
     */
    @PostMapping("/search")
    public R<StandardQueryResult> search(@Valid @RequestBody StandardQueryRequest request) {
        return R.ok(standardQueryService.search(request));
    }

    /**
     * 返回 DSL 专用校验契约，避免调用方只能从通用 message 中猜支持字段和输入边界。
     *
     * @param ex DSL 校验异常。
     * @return 兼容 {@link R} 信封的 validationError payload。
     */
    @ExceptionHandler(StandardQueryValidationException.class)
    public ResponseEntity<R<StandardQueryValidationError>> handleValidation(StandardQueryValidationException ex) {
        R<StandardQueryValidationError> response = R.ok(ex.getValidationError());
        response.setCode(400);
        response.setMessage(ex.getValidationError().message());
        response.setError(new ErrorDetail(
                ex.getValidationError().code(),
                "VALIDATION",
                true,
                "检查 Standard Query target/filter/op/value/limit；支持字段: "
                        + String.join(", ", ex.getValidationError().supportedFields()),
                "openspec/changes/add-standard-query-dsl/specs/standard-query-dsl/spec.md",
                List.of(),
                null,
                "standard-query-dsl",
                Map.of("bounds", ex.getValidationError().bounds()),
                List.of(
                        "使用 supportedFields 中的字段重新构造 filters。",
                        "将 limit 调整到 bounds 允许范围内。",
                        "如需非 FIELD target，先按 FIELD 查询或新开 DSL 扩展。")));
        return ResponseEntity.badRequest().body(response);
    }
}
