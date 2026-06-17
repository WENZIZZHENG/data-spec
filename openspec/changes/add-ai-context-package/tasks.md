## 1. OpenSpec 与测试

- [x] 1.1 验证 `add-ai-context-package` OpenSpec artifacts 通过 `openspec validate`。
- [x] 1.2 新增 AI Context zip 包服务单元测试，并先观察缺失方法导致的失败。

## 2. 后端实现

- [x] 2.1 在 `AiContextExportService` 中新增 zip 包生成方法和固定 entry 清单。
- [x] 2.2 生成 `field-catalog.schema.json`、`prompts.md`、`AGENTS.md.fragment` 和 good/bad SQL 示例内容。
- [x] 2.3 在 `AiContextController` 中新增 `dataspec-ai-context.zip` 下载接口。

## 3. 文档与验证

- [x] 3.1 更新 README 的 AI Context 包导出说明。
- [x] 3.2 运行后端测试，确保新增包结构测试和既有 lint 测试通过。
- [x] 3.3 运行 OpenSpec validate，确认 artifacts 与实现范围一致。
- [x] 3.4 进行直接代码评审，检查 zip entry、编码、响应头和兼容性。
- [x] 3.5 通过验证后提交本功能改动。
