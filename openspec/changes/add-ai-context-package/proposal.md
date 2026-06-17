## Why

DataSpec 当前只能分别导出 `DATABASE_RULES.md`、`field-catalog.json` 和 `rules.yaml`，AI 编程工具需要一次性复制到业务仓库的完整上下文包。P0 主线要求先让 Codex/Cursor/Claude Code/CI 能稳定读取字段标准，而不是只面向后台页面使用。

## What Changes

- 新增 AI Context zip 包导出能力，下载文件名为 `dataspec-ai-context.zip`。
- zip 内固定输出 `.dataspec/` 目录结构，包含规则文档、字段目录、字段目录 JSON Schema、规则 YAML、Prompt 模板和 good/bad SQL 示例。
- zip 根目录输出 `AGENTS.md.fragment`，用于复制到业务项目的 coding agent 指令中。
- 保留现有三个单文件预览/下载接口，不做破坏性变更。
- 本变更不自动修改外部业务仓库，不引入 CLI/MCP。

## Capabilities

### New Capabilities

- `ai-context-package`: 定义 DataSpec 为 AI 编程工具生成可复制上下文 zip 包的可观察行为。

### Modified Capabilities

- 无。

## Impact

- 后端：扩展 `dataspec-server/src/main/java/com/dataspec/aicontext` 的导出服务和 Controller。
- 测试：新增 AI Context 包结构和 zip 内容单元测试。
- 文档：更新 README 中 AI Context 导出包用法和验证命令。
- API：新增下载接口，返回 `application/zip` 二进制响应。
