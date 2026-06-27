## 1. OpenSpec 与后端测试基线

- [x] 1.1 验证 `add-field-impact-analysis` OpenSpec artifacts 通过 `openspec validate`。
- [x] 1.2 新增字段影响分析服务单测，覆盖空影响、跨项目拒绝、模板引用、导入来源、SQL 检查记录命中和快照/Context 提示。

## 2. 后端影响分析实现

- [x] 2.1 新增字段影响模型：报告 summary、影响项、影响类型、影响严重级别和编辑 warning。
- [x] 2.2 新增字段影响服务，校验 `projectId + fieldId`，实时聚合模板、反向导入来源、SQL 检查记录和标准快照影响。
- [x] 2.3 新增 `/api/fields/{id}/impact?projectId=` 只读接口，返回字段影响报告。
- [x] 2.4 确认影响分析不新增表、不写业务数据、不泄漏无关项目数据。

## 3. 前端字段库接入

- [x] 3.1 更新前端类型与 `src/api/field.ts`，新增字段影响报告 API wrapper。
- [x] 3.2 在字段库新增“影响”入口和详情弹窗，展示 summary、影响项和 warning。
- [x] 3.3 在编辑关键字段前展示非阻断影响提示，用户确认后仍可保存。
- [x] 3.4 补充前端展示工具函数或最小测试，覆盖影响类型标签、warning 文案和报告摘要。

## 4. 文档与待办

- [x] 4.1 更新 README 已实现能力和使用说明。
- [x] 4.2 更新 TODO，将 P6-6 标记为已完成第一版，并把下一步顺序推进到 P6-7。

## 5. 验证与评审

- [x] 5.1 运行后端 `mvn test`。
- [x] 5.2 运行前端 `pnpm test` 和 `pnpm build`。
- [x] 5.3 运行 `npx openspec validate add-field-impact-analysis` 和 `git diff --check`。
- [x] 5.4 进行直接代码评审并修复 findings，不使用子 agent。
