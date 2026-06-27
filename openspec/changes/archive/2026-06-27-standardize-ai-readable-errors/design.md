# 设计

## 方案

第一版采用兼容式扩展：

- 后端 `R<T>` 增加可选 `error` 字段，不改变成功响应，也不删除 `code/message/data`。
- 新增 `ErrorDetail` 和 `ErrorCatalog`：
  - `ErrorDetail` 包含 `code`、`category`、`retryable`、`suggestedAction`、`docsRef`。
  - `ErrorCatalog` 根据 HTTP/业务 code 与 message 进行保守分类，覆盖 token、项目权限、projectId、资源不存在、SQL 输入、数据库连接、校验失败和内部错误。
- `GlobalExceptionHandler` 使用 `R.fail(code, message)` 时自动填充 `error`。
- CLI `readJsonResponse` 在 HTTP 非 2xx 或业务 `code != 200` 时解析后端 `error`，抛出带诊断的错误；顶层 catch 输出 `DataSpecError: {...}`。
- MCP `readDataSpecJson` 把后端诊断放进 JSON-RPC error `data.dataspecError`。

## 取舍

- 不逐个改造所有 `new BizException(...)`。第一版通过集中分类器从现有 message/code 生成稳定诊断，降低改动面。
- 不改变 HTTP status 语义。`401/403/404/400/500` 仍由现有异常处理决定。
- 不要求前端立即迁移错误展示。前端请求拦截器仍可读取 `message`，后续 P6-39 再统一状态和错误动作。

## 风险与缓解

- message 启发式分类可能不完美：仅覆盖高频可恢复场景，默认落到 `VALIDATION_FAILED` 或 `INTERNAL_ERROR`，避免误导为具体错误。
- CLI stderr 增加结构化行可能影响脚本：原有 `错误: ...` 行保留，新增 `DataSpecError:` 是增量信息。
- MCP error data 可能被部分客户端忽略：JSON-RPC `message/code` 保持原样，`data` 只供支持结构化错误的客户端读取。
