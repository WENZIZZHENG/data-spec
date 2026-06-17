## Context

后端已提供 `/api/lint` 和 `/api/ai-context/package/download`。直接在 Java 后端内做 CLI 会牵涉 Spring Boot 启动、数据库连接、Flyway 和打包入口；第一版更适合用轻量 Node wrapper 调用现有 API，先解决 AI/CI 可调用问题。

## Goals / Non-Goals

**Goals:**
- 提供无需额外依赖的 Node CLI 脚本。
- 支持文件路径和 stdin 输入 SQL。
- 支持 JSON 输出和基于 `errorCount` 的退出码。
- 支持下载 AI Context zip 到指定路径。
- 单元测试通过 fake fetch 覆盖成功、错误退出和下载写入。

**Non-Goals:**
- 不提供离线 lint，不内嵌 Java 规则引擎。
- 不发布 npm/Maven/Homebrew 包。
- 不实现 DDL 生成或字段推荐命令。
- 不负责启动或管理后端服务。

## Decisions

- **HTTP-backed CLI。** 复用当前后端 API，避免重复规则逻辑；代价是运行前需要后端可访问。
- **Node ESM 单文件。** 项目已有 Node/pnpm 前端环境，Node 18+ 有内置 `fetch`，可以不增加依赖。
- **可测试 `runCli`。** CLI 文件导出 `runCli(argv, io, fetchFn)`，测试注入 fake fetch 和临时目录，命令行执行时才调用 `process.exitCode`。
- **退出码语义简单。** lint 请求成功但 `errorCount > 0` 返回 1；网络/API/参数错误返回 2；无 error 只含 warning/suggestion 返回 0。

## Risks / Trade-offs

- **[Risk] 后端未启动时 CLI 不可用。** → 错误信息明确提示 server 地址和失败原因；README 标明需要先启动后端。
- **[Risk] JSON 输出未来扩展。** → 直接输出后端 `LintResult`，减少 CLI 自定义字段漂移。
- **[Risk] Windows stdin/路径兼容。** → 使用 Node 标准 `fs.readFile` 和 fd `0`，测试覆盖文件输入；stdin 在 shell 中按 Node 默认行为工作。

## Migration Plan

新增脚本不影响现有后端和前端。后续若要发布真正 `dataspec` 命令，可把该脚本作为入口或迁移为独立包，保持命令参数兼容。
