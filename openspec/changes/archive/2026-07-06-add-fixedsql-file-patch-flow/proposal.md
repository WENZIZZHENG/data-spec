## Why

SQL lint 已经能返回 `fixedSql` 与 `fixedSqlDiff`，但 AI 或用户要把修复落到业务 SQL 文件时仍只能手工复制。直接覆盖文件风险高，需要一个默认 dry-run、可审查 unified diff、显式确认 apply、并能解释冲突和回退方式的文件补丁闭环。

## What Changes

- 新增 fixedSql 文件级补丁流程，支持从 lint 结果和目标文件生成补丁计划。
- 新增 CLI 命令用于预览补丁，输出 `unifiedDiff`、`conflictWarnings`、`dryRunResult`、`applyCommand`、`rollbackHint` 和 `evidenceRef`。
- 新增 CLI 确认应用模式；默认只 dry-run，只有显式确认参数才写入业务 SQL 文件。
- 补充本地文件写入安全约束：拒绝行号漂移、内容不匹配、路径越界、缺少 fixedSql 或补丁无变化的静默覆盖。
- 补充测试、契约 fixture 和验证记录，确保 AI 可机器读取补丁计划并在人工确认后再 apply。

## Capabilities

### New Capabilities
- `fixedsql-file-patch-flow`: fixedSql 文件级补丁计划、dry-run 输出、显式 apply、冲突诊断和回退提示。

### Modified Capabilities
- `dataspec-cli`: 新增 CLI patch 命令、参数、JSON 输出字段和退出码语义。
- `ai-write-safety-protocol`: 将本地业务仓库文件写入纳入 AI 写入安全边界，要求默认 dry-run、显式确认和非敏感诊断。

## Impact

- `tools/` CLI 与测试：新增 patch 计划生成、apply 辅助函数、CLI 分支和 Node 单测。
- CLI/MCP 契约 fixture：为新增 AI-facing CLI 命令补稳定输入/输出示例和安全 metadata。
- 文档/TODO/OpenSpec：记录 P6-78 能力、边界和验证证据。
- 不改变后端 OpenAPI、数据库 schema、SQL lint 响应契约或现有 `lint` / `lint-files` 输出形状。
