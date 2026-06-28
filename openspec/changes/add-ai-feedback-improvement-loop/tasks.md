## 1. OpenSpec 与范围确认

- [x] 1.1 验证 proposal/design/spec/tasks 完整且 `openspec validate add-ai-feedback-improvement-loop` 通过
- [x] 1.2 确认第一版只读聚合边界：不新增反馈事件表、不采集用户行为、不自动写标准变更

## 2. 后端反馈报告 API

- [ ] 2.1 新增 `aifeedback` 模块模型：summary、fieldSignals、ruleSignals、fixedSqlSignals、unmanagedSignals、nextActions、sampleSize、generatedAt
- [ ] 2.2 补充必要 repository 只读查询方法，读取最近 AI job、SQL 检查记录、规则例外、字段来源和字段元数据
- [ ] 2.3 实现 `AiFeedbackService` 聚合逻辑，包含项目访问校验、敏感信息脱敏、推荐历史不足缺口说明和 targetRoute
- [ ] 2.4 新增 `/api/ai-feedback/report?projectId=` Controller
- [ ] 2.5 补后端单测，覆盖项目隔离调用、规则排行、fixedSql 信号、字段引用信号、推荐历史缺口和脱敏

## 3. 前端反馈页面

- [ ] 3.1 同步或手工补齐 OpenAPI/前端类型与 `src/api/aiFeedback.ts`
- [ ] 3.2 新增“AI 反馈”页面和导航入口，展示 summary、信号列表、下一步动作、无项目空状态和刷新
- [ ] 3.3 支持信号 targetRoute 跳转到字段库、字段质量、规则配置、规则例外、SQL 校验或 AI 回放
- [ ] 3.4 补前端 utility/smoke 测试，覆盖状态标签、路由/API/页面关键文案和无项目 guard

## 4. 文档、验证与提交

- [ ] 4.1 更新 README/TODO 中 P6-27 状态、使用说明和边界
- [ ] 4.2 运行 `mvn test`、`pnpm test`、`pnpm build`、OpenSpec validate 和 `git diff --check`
- [ ] 4.3 执行本地结构化代码评审并修复 findings，不使用子 agent
- [ ] 4.4 创建本地 commit，提交 P6-27 实现
