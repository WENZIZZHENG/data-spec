## Context

现有 `tools/dataspec-cli.mjs` 已提供 `lint`、`lint-files`、`review-pr`、`export-context`、`suggest-field` 和 `generate-ddl`，并通过 `tools/dataspec-config.mjs` 从当前目录向上读取 `.dataspec/config.json`。前端已有 `dataspec-web/scripts/check-openapi-schema.mjs` 做 OpenAPI schema 漂移检查，但 CLI 没有一个统一入口告诉 AI agent 当前环境是否可用。

## Goals / Non-Goals

**Goals:**

- 新增 `doctor` 命令，复用现有 CLI 配置优先级：显式参数 > 环境变量 > `.dataspec/config.json` > 默认值。
- 检查配置、服务连通、token 身份、项目可访问性、默认扫描路径和 OpenAPI 状态。
- 同时支持可读文本输出和 JSON 输出，供人和 AI agent 使用。
- 用 Node 单测覆盖成功、失败和 JSON 输出，接入现有 `node --test` 验证入口。

**Non-Goals:**

- 不新增后端 health endpoint，不修改数据库或安全模型。
- 不自动修复 `.dataspec/config.json`，不写入业务仓库。
- 不默认执行完整 OpenAPI 生成漂移检查，避免日常 `doctor` 依赖 `openapi-typescript`；完整漂移检查通过显式 `--check-openapi` 启用。
- 不改变现有 CLI 命令的参数、输出和退出码。

## Decisions

1. **在现有 CLI 中新增 `doctor` 分支**
   - 理由：DataSpec CLI 已是 AI/CI 调用入口，`doctor` 应复用同一套参数解析、config loader、token header 和测试注入方式。
   - 替代方案：新增独立脚本；会复制配置和 token 解析逻辑，容易漂移。

2. **检查项统一为结构化 `checks[]`**
   - 理由：AI agent 需要稳定 JSON，而人类需要可扫读文本；内部统一结构后两种输出只做格式化。
   - 状态枚举使用 `pass`、`warn`、`fail`。`fail` 影响退出码，`warn` 只提示风险。

3. **默认 OpenAPI 检查轻量化，完整漂移检查显式启用**
   - 默认检查 `/api-docs` 是否可达、`dataspec-web/src/api/schema.ts` 是否存在。
   - `--check-openapi` 时复用 `checkOpenapiSchema` 执行生成对比，失败时输出明确建议。
   - 理由：日常 doctor 应快速、少依赖；CI 或用户需要强保证时再承担生成成本。

4. **失败检查返回 1，命令异常返回 2**
   - 理由：沿用现有 CLI “业务检查失败”和“命令无法执行”分离的风格，便于 CI 与 AI 决策。
   - 示例：服务不可达、项目不存在、token 无权限属于检查失败；未知参数、非法 projectId 属于命令错误。

## Risks / Trade-offs

- **OpenAPI 完整漂移检查较慢或依赖缺失** → 默认不启用，`--check-openapi` 失败时作为 `openapi` 检查失败返回，提示运行 `pnpm gen:api` 或安装依赖。
- **安全模式关闭时 token 身份可能是本地默认身份** → `auth` 检查只展示当前身份摘要，真正授权以项目详情请求能否成功为准。
- **项目接口返回 404/403 时错误信息来自后端** → 统一包装为 project 检查失败，保留 HTTP 状态或后端 message。
- **默认路径不存在** → 作为 `defaultPaths` 失败检查，避免后续 `lint-files` 才暴露问题。
