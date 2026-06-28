## 1. OpenSpec 与数据模型

- [x] 1.1 验证 proposal/design/spec/tasks 完整且 `openspec validate add-ai-batch-delivery-package` 通过
- [ ] 1.2 新增 Flyway 迁移 `ds_ai_batch_run`，保存 projectId、batchType、source、status、summaryJson、payloadJson、operatorName、createdAt、updatedAt
- [ ] 1.3 新增 batch run entity、mapper、repository、DTO 和 package 模型，字段名对齐 spec 中稳定契约

## 2. 后端批量运行 API

- [ ] 2.1 新增 `AiBatchService`，支持同步 SQL lint items 聚合、逐项失败保留、summary/evidence/nextActions 生成和 payload 脱敏
- [ ] 2.2 新增 `/api/ai-batches/sql-lint`、列表、详情和下载 JSON API
- [ ] 2.3 补后端 service/controller 单测，覆盖成功聚合、单项失败、下载包、分页列表和敏感信息不外泄

## 3. CLI 交付包输出

- [ ] 3.1 抽取或新增 CLI delivery package builder，复用 `lint-files` 扫描结果生成稳定 packageVersion/summary/items/evidence/nextActions
- [ ] 3.2 为 `lint-files` 增加显式交付包输出参数，保持原 `--format json` 输出和退出码兼容
- [ ] 3.3 补 Node 测试覆盖 package 文件写入、兼容输出和敏感字段剔除

## 4. 前端页面与类型

- [ ] 4.1 同步或手工补齐 OpenAPI/前端类型与 `src/api/aiBatch.ts`
- [ ] 4.2 新增“AI 批量任务”页面和导航入口，展示最近 run、summary、详情、下载 JSON 和无项目空状态
- [ ] 4.3 补前端 utility/smoke 测试，覆盖批量任务页面、API 耦合和关键文案

## 5. 文档、验证与提交

- [ ] 5.1 更新 README/TODO 中 P6-26 状态和使用说明
- [ ] 5.2 运行 `mvn test`、`pnpm test`、`pnpm build`、`node --test tools/dataspec-cli.test.mjs tools/dataspec-mcp.test.mjs`、OpenSpec validate 和 `git diff --check`
- [ ] 5.3 执行本地结构化代码评审并修复 findings，不使用子 agent
- [ ] 5.4 创建本地 commit，提交 P6-26 实现
