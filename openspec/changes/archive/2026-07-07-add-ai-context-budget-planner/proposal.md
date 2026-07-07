## Why

AI Context 已支持按 scope/query/limit 导出，但用户和 AI 仍需要先知道“给定任务和预算下，这份上下文够不够、哪些内容会被舍弃、风险是什么”。P6-90 需要补一个确定性的上下文预算 planner，让个人/小团队在导出完整包、标准包或极简包前能看到 token 估算、裁剪取舍和低预算降级提示。

## What Changes

- 新增 AI Context budget planner：输入 projectId、taskType/profile、query、目标表/文件和 tokenBudget，输出 selectedArtifacts、droppedArtifacts、estimatedTokens、qualityRisk、fallbackSteps 和 recommendedNextActions。
- 新增只读后端 API 和 CLI `context-budget plan`，不调用外部 LLM，不上传标准内容到外部服务，不写入项目状态。
- 扩展前端 AI Context 页面，在预览/下载前展示预算预估、裁剪策略、质量风险和建议导出参数。
- 更新 CLI/MCP contract fixture、README/TODO、OpenSpec Evidence 和相关测试，确保 AI 能稳定消费 planner 输出。
- 不实现模型专属精确 tokenizer，不自动导出或覆盖用户选择，不保证低预算包覆盖所有复杂任务。

## Capabilities

### New Capabilities
- `ai-context-budget-planner`: 覆盖 AI Context 预算估算、裁剪建议、质量风险、降级动作和只读安全边界。

### Modified Capabilities
- `ai-context-scoped-export`: 前端和导出流程可在导出前读取 budget planner 建议，但既有 scoped export 行为保持兼容。
- `dataspec-cli`: 新增 `context-budget plan` 命令、稳定 JSON 输出、退出码、安全 metadata 和 contract fixture 覆盖。

## Impact

- 后端：新增 AI Context budget planner model/service/controller；复用现有字段目录、规则、使用示例和 profile/scope 语义，只读估算。
- CLI/tools：新增 `context-budget plan` 子命令、契约 fixture 和 Node 测试。
- 前端：AI Context 页面新增预算输入、planner 预览区和风险提示；继续使用现有导出/预览入口。
- 文档/OpenSpec：新增 change artifacts，更新 README/TODO 和 Verification Evidence。
- 安全边界：planner 只返回摘要、计数、估算和建议；不得输出 token、password、Authorization、完整 JDBC URL、DSN、连接串或业务数据行。
