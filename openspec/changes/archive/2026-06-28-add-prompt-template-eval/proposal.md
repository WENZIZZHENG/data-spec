## Why

AI Prompt、DDL 生成和 SQL 修复提示已经进入核心链路，但模板文本、版本号和样例约束仍分散在实现里。缺少统一 registry 与评测入口时，后续微调 prompt 可能悄悄破坏输出格式、字段引用或 AI 回放可解释性。

## What Changes

- 新增 Prompt 模板 registry，集中声明模板 key、版本、场景、输出约束、必备段落和变更说明。
- 新增本地 Prompt 模板评测能力，使用 fixture/golden 断言模板输出是否满足契约，并在文本变化时给出可读 diff。
- Prompt 生成、SQL lint/fixedSql 和 DDL preview 的 AI job 记录统一从 registry 引用 `promptVersion`。
- AI 契约测试覆盖 Prompt 模板稳定字段和模板评测，破坏 JSON/Markdown/SQL 输出约束时失败。

## Capabilities

### New Capabilities
- `prompt-template-registry`: 管理 AI prompt/生成任务模板的版本、场景、输出约束和本地评测。

### Modified Capabilities
- `ai-prompt-generator`: 生成 create-table 与 fix-sql prompt 时暴露并记录 registry 中的模板版本。
- `ai-decision-replay`: AI job 记录中的 `promptVersion` 必须可追溯到 registry 模板版本。
- `ddl-generator-tool`: DDL preview 记录使用 registry 中的生成模板版本。
- `ai-contract-fixtures`: 现有验证入口必须覆盖 Prompt 模板契约和评测 fixture。

## Impact

- 后端新增 prompt 模板 registry、评测模型/服务/API 或等价本地验证入口。
- 后端 prompt 生成、SQL lint AI 回放、DDL preview 回放改为引用统一版本常量。
- 测试资源新增 prompt fixture/golden 样例，并接入 `mvn test`。
- README/TODO 更新 P6-31 状态、用法和验证说明。

## Verification Evidence

- `mvn test`（`dataspec-server`）：275 tests, 0 failures, 0 errors。
- `node --test tools/prompt-template-eval.test.mjs`：4 tests, 0 failures。
- `node tools/prompt-template-eval.mjs --format json`：create-table/fix-sql prompt fixtures 均 `passed: true`。
- `npx.cmd openspec validate add-prompt-template-eval`：valid。
- `git diff --check`：退出码 0，仅 Windows 行尾提示。
- 结构化代码评审：不使用子 agent；已修复 registry 重复/版本格式防护、脚本参数缺值错误处理两个 findings。
