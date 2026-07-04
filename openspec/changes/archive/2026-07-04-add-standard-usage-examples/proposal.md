## Why

AI 生成 DDL、Prompt 或字段映射时，只读字段说明和规则文本仍容易误用标准字段；P6-60 需要把分散在测试、SQL fixture 和自由文本里的正例/反例沉淀成可维护、可裁剪的结构化示例库。

## What Changes

- 新增标准字段使用示例与反例库，按项目维护 `exampleType`、`scope`、`input`、`expectedOutput`、`antiPattern`、`reason`、`tags`、`priority` 等字段。
- 新增后端 API 和前端维护入口，用于列表、创建、更新、删除、启用/停用示例。
- AI Context 导出时按 scope/query/limit 裁剪少量高价值示例，输出机器可读 JSON，并在 README 中提示 AI 使用方式。
- 增加 fixture/快照类测试，防止示例库输出格式漂移或导出敏感内容。

## Capabilities

### New Capabilities
- `standard-usage-examples`: 结构化维护标准字段/规则/模板使用正例和反例，并提供 API 与前端入口。

### Modified Capabilities
- `ai-context-package`: AI Context 包需要包含裁剪后的标准使用示例与反例文件。
- `ai-context-scoped-export`: scoped export 需要把同一 scope/query/limit 应用于示例/反例裁剪。
- `frontend-task-entrypoints`: 前端需要在标准维护或 AI 使用相关区域提供示例库入口。

## Impact

- 后端：新增示例库表、实体、Repository、Service、Controller，并扩展 AI Context 导出服务。
- 前端：新增 API 封装、类型导出和示例库页面/入口。
- OpenAPI/契约：重新生成 `dataspec-web/src/api/schema.ts`，新增示例库 DTO 类型。
- 测试：新增后端 service/controller/AI Context 导出测试与前端冒烟门禁覆盖。

## Verification Evidence

- `mvn test`（`dataspec-server`）：410 tests, 0 failures, 0 errors；Maven 仍输出本地 `javax.annotation-api/jvnet-parent` 无效 POM 警告，不影响测试结果。
- `pnpm test`（`dataspec-web`）：102 tests, 0 failures。
- `pnpm build`（`dataspec-web`）：`vue-tsc --noEmit && vite build` 通过；Vite 仍输出既有 chunk size / Rolldown pure annotation warning。
- `pnpm check:api`（`dataspec-web`，临时启动后端并关闭 Flyway）：`OpenAPI schema.ts 已是最新: src/api/schema.ts`。
- `openspec validate add-standard-usage-examples --strict`：Change valid。
- `git diff --check`：exit 0，仅提示工作区 CRLF 转换 warning。
- 独立 agent code review：首轮发现 4 个 AI Context 示例筛选/截断/`contextScope` 问题；已补测试并修复，复查结论为“复查未发现阻塞性问题”。
