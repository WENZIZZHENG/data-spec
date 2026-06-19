## Why

前端已经提交了 `src/api/schema.ts` 生成产物，但后端接口变更后仍可能忘记执行 `pnpm gen:api`，导致类型契约漂移。P4-5 需要一个可在本地和 CI 使用的门禁，尽早发现 OpenAPI 与仓库类型产物不一致。

## What Changes

- 新增 OpenAPI schema 漂移检查脚本：重新生成临时 `schema.ts` 并与仓库产物比较。
- 脚本默认读取 `http://localhost:8090/api-docs`，也支持 `--source` 或环境变量指定本地/远端 OpenAPI 文档。
- 前端 `package.json` 增加 `check:api` 脚本。
- 增加轻量 Node 测试，覆盖参数解析、文本比较和错误输出核心逻辑。
- README/TODO 补充验证命令和 P4-5 状态。

## Capabilities

### New Capabilities

- `openapi-schema-drift-check`: 检查前端 OpenAPI 生成类型是否与当前 API 文档一致。

### Modified Capabilities

无。

## Impact

- 主要影响 `dataspec-web/scripts/check-openapi-schema.mjs`、`dataspec-web/package.json` 和测试。
- 复用已有 `openapi-typescript` devDependency，不新增运行时依赖。
- 不修改后端接口、前端 API 封装或生成产物内容。
