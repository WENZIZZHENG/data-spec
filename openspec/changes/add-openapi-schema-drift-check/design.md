## Context

`dataspec-web` 通过 `openapi-typescript http://localhost:8090/api-docs -o src/api/schema.ts` 生成 API 类型。生成产物已提交到仓库，优点是前端构建不依赖后端启动；缺点是后端契约变更时，开发者可能忘记更新生成产物。

## Goals / Non-Goals

**Goals:**

- 提供 `pnpm check:api`，重新生成临时 schema 并与 `src/api/schema.ts` 比较。
- 默认使用 `http://localhost:8090/api-docs`，同时允许 `--source` 和 `DATASPEC_API_DOCS_URL` 覆盖。
- 支持 `--schema` 覆盖目标 schema 文件，方便测试。
- 输出清晰错误，指导开发者运行 `pnpm gen:api`。
- 为脚本中可纯测的参数解析、比较逻辑补 Node 测试。

**Non-Goals:**

- 不新增 OpenAPI runtime client。
- 不强制 `pnpm build` 自动执行该检查。
- 不在本轮搭建完整 CI workflow。
- 不修改 `src/api/schema.ts` 的现有生成内容。

## Decisions

1. **脚本生成到临时目录再比较**
   - 理由：不污染工作区，失败时不会改写用户文件。
   - 替代方案：直接覆盖 `src/api/schema.ts` 后看 git diff；风险更高，容易留下半成品。

2. **复用 CLI `openapi-typescript`**
   - 理由：与 `pnpm gen:api` 使用同一生成器，避免程序化 API 变动带来额外适配。
   - 替代方案：直接 import `openapi-typescript`；API 更依赖包内部形态，长期稳定性不如 CLI。

3. **URL 与本地文件都支持**
   - 理由：本地开发可连正在运行的后端，CI 可使用预先导出的 api-docs 文件。
   - 替代方案：只支持 URL；CI 不稳定，和 P4-5 的目标冲突。

## Risks / Trade-offs

- **后端未启动** → 脚本失败并提示 `--source` 或 `DATASPEC_API_DOCS_URL`。
- **生成器版本变化导致格式差异** → 使用项目 lockfile 中已有 devDependency，变更会体现在 diff 中。
- **换行差异误报** → 比较前统一 CRLF/LF，避免 Windows 环境误报。
