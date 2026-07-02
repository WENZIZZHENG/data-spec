## 1. 草稿确认

- [x] 1.1 人工确认 OpenSpec 草稿中的 change id、capability、验收标准和边界。
- [x] 1.2 补充或删除 Open Questions，确保需求可实施。

## 2. 测试先行

- [x] 2.1 新增后端失败测试：创建健康快照聚合字段质量、AI 反馈、候选/规则/SQL 统计，且 payload 不包含 SQL/token/连接串。
- [x] 2.2 新增后端失败测试：趋势查询返回最近快照和 week/month delta；空趋势返回可恢复 nextActions。
- [x] 2.3 新增前端失败测试：标准健康页面/API smoke 覆盖入口、创建快照和复制计划。
- [x] 2.4 运行失败测试，确认失败原因来自功能缺失。

## 3. 实现

- [x] 3.1 新增 Flyway 迁移、实体/Mapper/Repository，保存 `ds_standard_health_snapshot`。
- [x] 3.2 新增模型、Service 和 Controller：`POST /api/standard-health/snapshots`、`GET /api/standard-health/trend`、`GET /api/standard-health/plan`。
- [x] 3.3 复用字段质量、AI 反馈、候选、规则例外和 SQL 检查记录，生成 top actions、Markdown plan 和脱敏 payload。
- [x] 3.4 更新 OpenAPI 前端类型/API 封装，新增标准健康趋势页和导航入口。
- [x] 3.5 更新 README/TODO 或相关文档，记录第一版能力和边界。

## 4. 验证与收口

- [x] 4.1 运行 `openspec validate <change-id> --strict`。
- [x] 4.2 运行与改动范围匹配的验证命令，并记录证据。
- [x] 4.3 执行本地结构化代码评审并修复 findings，不使用子 agent。
- [x] 4.4 完成提交并归档 OpenSpec change。

## Verification Evidence

- `openspec validate add-standard-health-trends --strict`：通过。
- `openspec validate --all`：90 items passed，0 failed。
- `mvn test`：373 tests，0 failures，0 errors。
- `pnpm gen:api`：从 `http://localhost:8090/api-docs` 重新生成 `dataspec-web/src/api/schema.ts`。
- `pnpm check:api`：通过，生成 schema 与当前 `/api-docs` 一致。
- `pnpm test`：98 tests，0 failures。
- `pnpm build`：通过；仅有依赖包 Rolldown pure annotation 与 chunk size 警告。
- `git diff --check`：仅报告本仓库既有 Windows LF/CRLF 换行提示，无 whitespace error。
- 本地结构化评审：发现并修复 `StandardHealth.vue` Top action 跳转未保留原 query 的问题；修复后重新运行 `pnpm test` 和 `pnpm build` 通过，无剩余阻断 findings。
