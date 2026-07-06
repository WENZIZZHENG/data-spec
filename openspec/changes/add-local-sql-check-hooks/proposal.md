## Why

CI 和 PR review 能发现 SQL 标准问题，但反馈点已经偏晚。P6-91 需要把已有 `lint-changed` 能力前移到本地 pre-commit 和 IDE 保存前检查，让个人/小团队在提交前就获得 AI 可读、可跳转、可继续修复的 DataSpec 诊断。

## What Changes

- 新增本地 SQL 检查安装能力：CLI 提供 `install-hook`，在业务仓库内生成 DataSpec 管理的 `pre-commit` hook。
- 新增可选 VS Code 任务与 Problem Matcher 示例，复用 `lint-changed --format text`，便于保存前或手动任务运行后定位 file/line/rule/severity/suggestion。
- `install-hook` 输出稳定 JSON/text，包含写入/跳过的产物、保护性诊断、建议命令、安全 metadata 和下一步动作。
- hook 安装不强制启用所有项目，不替代 CI/GitHub Review，不调用外部 LLM，不写入 token/password，不覆盖非 DataSpec 管理的用户 hook。
- 更新 CLI/MCP contract fixture、README/TODO、OpenSpec Evidence 和 Node 测试，防止本地工作流契约漂移。

## Capabilities

### New Capabilities
- `local-sql-check-hooks`: 覆盖本地 pre-commit hook、VS Code task/problem matcher 示例、安装输出、安全边界和失败诊断。

### Modified Capabilities
- `dataspec-cli`: 新增 `install-hook` 命令、稳定输出、退出码、安全 metadata 和 help 文本。
- `cli-mcp-contract-fixtures`: 新增 `install-hook` fixture，覆盖输入边界、输出 shape、安全约束、示例和 recommended next actions。

## Impact

- CLI/tools：新增 hook 安装逻辑、VS Code 示例生成、Node 测试和 contract fixture。
- 文档/OpenSpec：新增 change artifacts，更新 README/TODO，记录本地启用方式和边界。
- 安全边界：只写当前业务仓库内 `.git/hooks/pre-commit` 和可选 `.vscode/` 示例；不写凭据，不覆盖非托管 hook，不修改远端仓库、不绕过用户 Git 配置。
