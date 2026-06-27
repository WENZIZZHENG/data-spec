## 1. OpenSpec 与测试

- [x] 1.1 验证 `add-ai-prompt-generator` OpenSpec artifacts 通过 `openspec validate`。
- [x] 1.2 新增服务测试，并先观察缺失 prompt 方法导致失败。
- [x] 1.3 新增控制器测试，并先观察缺失 prompt API 导致失败。

## 2. 实现

- [x] 2.1 在 `AiContextExportService` 新增建表 prompt 和修正 prompt 方法。
- [x] 2.2 在 `AiContextController` 新增两个 POST API。
- [x] 2.3 更新 README/TODO 与 OpenAPI TypeScript schema。

## 3. 验证

- [x] 3.1 运行后端测试、前端构建、OpenSpec validate 和 diff 空白检查。
- [x] 3.2 进行直接代码评审，检查 prompt 内容来源、lint 问题嵌入、API 请求体和边界。
- [x] 3.3 通过验证后提交本功能改动。
