## 1. OpenSpec 与现状基线

- [x] 1.1 校验 OpenSpec change，确认 proposal/design/spec/tasks 格式有效。
- [x] 1.2 梳理目标页面现有 query、详情弹窗、筛选和复制能力，确定第一版 URL 参数白名单。

## 2. URL 状态工具与全局项目联动

- [x] 2.1 新增 `src/utils/urlState.ts`，提供 query 解析、白名单合并、敏感 key 剔除、无变化 replace 和复制当前链接工具。
- [x] 2.2 新增 `tests/urlState.test.ts`，覆盖正整数、字符串、枚举、数组/坏值兜底、敏感参数剔除和 query 合并。
- [x] 2.3 改造 `App.vue`/project store 交互：打开带 `projectId` 的链接可切换当前项目，顶部切换项目时同步安全 query。

## 3. 关键页面 URL 状态接入

- [x] 3.1 `FieldLibrary.vue` 扩展字段库链接：保持 `keyword`/`fieldId`，补复制链接入口和无效 fieldId 兜底。
- [x] 3.2 `SqlLint.vue` 接入 `recordId`、历史页码和复制记录链接；打开链接时加载对应检查记录详情，关闭详情时清理 query。
- [x] 3.3 `AiReplay.vue` 接入 `jobType`、分页、`aiJobId` 和复制回放链接；无效详情 id 时提示并清理 query。
- [x] 3.4 `FieldCoverage.vue` 接入 `table`、`status` 和复制筛选链接；报告生成后按 URL 筛选恢复。
- [x] 3.5 `ReverseImport.vue` 接入 `sourceBatchId`、`table`、`status` 和复制来源/批次链接；不写入连接表单、SQL 或凭据。

## 4. 文档、测试与收口

- [x] 4.1 更新 `tests/frontendSmoke.test.ts`，锁定目标页面的 URL 状态工具、复制链接入口和关键 query 接线。
- [x] 4.2 更新 README、TODO 和必要前端说明，记录可复制链接范围与敏感信息边界。
- [x] 4.3 运行 `openspec validate add-frontend-url-state-links --strict`、`pnpm test`、`pnpm build` 和 `git diff --check`。
- [x] 4.4 执行本地结构化代码评审并修复 findings，不使用子 agent。
- [x] 4.5 完成提交并归档 OpenSpec change。
