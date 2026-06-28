## Why

DataSpec 已经有 OpenAPI、CLI、MCP、workflow recipes、AI task profiles、schema registry 和 doctor，但 AI agent 第一次进入项目时仍需要从 README 或源码里拼凑“我能调用什么、需要哪些前置条件、失败后下一步是什么”。P6-43 要提供一个机器可读的能力清单，作为 AI 使用 DataSpec 的稳定自描述入口。

## What Changes

- 新增 AI capability catalog，列出 DataSpec 当前稳定开放给 AI/CLI/MCP 的核心任务能力、入口、输入输出契约、权限要求、前置检查、示例命令和推荐下一步。
- 后端新增只读能力清单 API，支持按项目返回全局能力和项目相关诊断，不执行任何任务、不写入业务数据。
- CLI 新增能力清单读取命令，MCP 新增 resource，让 agent 不打开 README 也能先发现可用能力。
- AI Context 导出包新增 `.dataspec/capabilities.json` 和 README 引导，离线缓存也能让 agent 看到最近一次导出的能力清单。
- README/TODO/OpenSpec 更新，说明能力清单是自描述目录，不替代 OpenAPI、Schema Registry、doctor 或权限控制。

## Capabilities

### New Capabilities
- `ai-capability-catalog`: DataSpec 面向 AI 的能力清单、自描述 API、能力条目契约和项目级诊断。

### Modified Capabilities
- `dataspec-cli`: CLI 暴露 capability list/show/check 或等价命令，输出稳定 JSON。
- `dataspec-mcp`: MCP 暴露 capability catalog resource，供 agent 启动时读取。
- `ai-context-package`: AI Context 包包含 capability catalog 文件，并在 manifest/README 中引用。

## Impact

- 后端：新增 capability catalog 模型、内置 registry、只读 controller/service 和单测。
- 前端/类型：重新生成 OpenAPI schema，必要时导出类型；第一版不新增完整前端页面。
- 工具：扩展 `tools/dataspec-cli.mjs`、`tools/dataspec-mcp.mjs` 及其 Node 测试。
- 文档/规范：更新 README、TODO、AI Context 相关说明和 OpenSpec 主规格。

## Verification Evidence

- `openspec validate add-ai-capability-catalog --strict`：通过。
- `mvn test`（`dataspec-server`）：通过，338 tests，0 failures，0 errors。
- `pnpm test`（`dataspec-web`）：通过，88 tests，0 fail。
- `pnpm build`（`dataspec-web`）：通过；仅保留 Rolldown `INVALID_ANNOTATION` 与 chunk size 既有 warning。
- `node --test tools\dataspec-config.test.mjs tools\dataspec-cli.test.mjs tools\dataspec-mcp.test.mjs`：通过，100 tests，0 fail。
- `git diff --check`：通过；仅输出 CRLF 工作区换行 warning。
- 本地结构化代码评审：发现并修复 1 个 CLI 契约问题，未知 `capability show <id>` 现在稳定返回 `CAPABILITY_NOT_FOUND` 参数诊断，并建议先运行 `capability list`。
