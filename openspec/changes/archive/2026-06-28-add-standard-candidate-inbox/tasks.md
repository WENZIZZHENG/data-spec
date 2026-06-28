## 1. OpenSpec 与范围确认

- [x] 1.1 验证 proposal/design/spec/tasks 完整且 `openspec validate add-standard-candidate-inbox` 通过
- [x] 1.2 确认第一版边界：不做审批流、不自动合并标准字段、不读取源数据库业务数据行

## 2. 后端候选 Inbox

- [x] 2.1 新增 Flyway 迁移 `ds_standard_candidate`，包含项目、候选字段、来源、证据、置信度、状态、决策和索引
- [x] 2.2 新增 `standardcandidate` 模块 entity/model/repository/service/controller
- [x] 2.3 实现候选列表、创建候选和项目访问校验，支持 status/sourceType/keyword 分页筛选
- [x] 2.4 实现 accept/merge/ignore/postpone 决策 API，accept 创建标准字段，merge 只记录目标字段，不静默改目标字段
- [x] 2.5 补后端单测，覆盖分页筛选、重复字段拒绝、接受创建字段、合并记录目标字段、忽略/延后原因和项目隔离

## 3. 前端候选工作台

- [x] 3.1 同步或手工补齐 OpenAPI/前端类型与 `src/api/standardCandidate.ts`
- [x] 3.2 新增“标准候选”页面和导航入口，展示候选列表、筛选、证据、状态和无项目空状态
- [x] 3.3 实现新建候选、接受、合并、忽略、延后交互，决策成功后刷新列表
- [x] 3.4 补前端 utility/smoke 测试，覆盖状态标签、路由/API/页面关键文案和无项目 guard

## 4. 文档、验证与提交

- [x] 4.1 更新 README/TODO 中 P6-28 状态、使用说明和边界
- [x] 4.2 运行 `mvn test`、`pnpm test`、`pnpm build`、OpenSpec validate 和 `git diff --check`
- [x] 4.3 执行本地结构化代码评审并修复 findings，不使用子 agent
- [x] 4.4 创建本地 commit，提交 P6-28 实现
