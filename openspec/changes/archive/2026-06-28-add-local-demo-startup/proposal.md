## Why

DataSpec 已有后端、前端、Flyway 和演示项目入口，但新机器仍需要手动创建数据库、分别启动服务并人工确认 demo 链路。P6-34 提供一套个人/小团队本地启动包，让用户和 AI agent 能用稳定命令启动可用环境并拿到可复制的 smoke 结果。

## What Changes

- 新增本地 Docker Compose 配置，启动 PostgreSQL，并为后端、前端提供一致的环境变量和端口约定。
- 新增本地启动/验证脚本，覆盖依赖检查、端口提示、服务健康检查和演示项目 seed smoke。
- README 补充一键启动、开发模式和 smoke 验证命令。
- 不新增业务 API，不改变演示项目数据模型，不引入生产级部署承诺。

## Capabilities

### New Capabilities

- `local-demo-startup`: 本地一键启动、健康检查和演示项目 smoke 验证。

### Modified Capabilities

无。

## Impact

- 根目录 Docker Compose、本地脚本、README/TODO 和 OpenSpec。
- 后端/前端运行配置只通过环境变量覆盖，不改默认开发配置。
- 依赖 Docker Compose、Maven、pnpm 和现有 `/api/projects/demo`、`/api/dashboard`、`/api/lint` 等接口。

## Verification Evidence

- `node --test tools/dataspec-local-smoke.test.mjs`：5 tests passed，覆盖参数解析、健康链路 JSON 输出、失败脱敏、通用脱敏和 compose/Vite 代理契约。
- `pnpm build`（`dataspec-web/`）：通过；仅保留现有 Rolldown `INVALID_ANNOTATION` 与 chunk size 警告。
- `npx.cmd openspec validate add-local-demo-startup`：通过。
- `git diff --check` / `git diff --cached --check`：通过；仅有 Windows LF/CRLF 提示。
- `docker compose -f docker-compose.local.yml config`：当前机器未安装 `docker` 或不在 PATH，无法执行实际 Compose 解析；已用源码级 compose 契约测试作为本环境兜底。
- 结构化代码评审：未发现必须修复的功能、安全、性能或文档问题；确认 smoke 输出不泄漏 token/password/JDBC，compose 文档明确限定为个人本地开发而非生产部署。
