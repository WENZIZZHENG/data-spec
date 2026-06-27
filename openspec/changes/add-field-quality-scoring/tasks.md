## 1. OpenSpec 与后端测试基线

- [x] 1.1 验证 `add-field-quality-scoring` OpenSpec artifacts 通过 `openspec validate`。
- [x] 1.2 新增字段质量评分服务单测，覆盖满分字段、缺元数据字段、疑似敏感未标记、枚举/状态字段缺代码集、废弃字段缺替代说明和空项目。

## 2. 后端质量评分实现

- [x] 2.1 新增字段质量模型：报告 summary、字段质量项、质量 issue、质量等级和 severity。
- [x] 2.2 新增字段质量服务，复用 `FieldService.listByProject(projectId)` 实时计算 score、level、issues 和 suggestions。
- [x] 2.3 新增 `/api/fields/quality?projectId=` 只读接口，返回项目级质量报告。
- [x] 2.4 确认质量评分不新增表、不写字段、不泄漏无关项目数据。

## 3. 前端质量视图

- [x] 3.1 更新前端类型与 `src/api/field.ts`，新增字段质量报告 API wrapper。
- [x] 3.2 新增字段质量页面，展示 summary、等级筛选、issue 筛选、按分数升序列表和跳转字段库编辑入口。
- [x] 3.3 更新路由和左侧导航，把页面放入数据管理或标准维护入口。
- [x] 3.4 补充前端展示工具函数或最小测试，覆盖等级标签、issue 过滤和跳转 query 构造。

## 4. 文档与待办

- [x] 4.1 更新 README 已实现能力和使用说明。
- [x] 4.2 更新 TODO，将 P6-5 标记为已完成第一版，并把下一步顺序推进到 P6-6。

## 5. 验证与评审

- [x] 5.1 运行后端 `mvn test`。
- [x] 5.2 运行前端 `pnpm test` 和 `pnpm build`。
- [x] 5.3 运行 `npx openspec validate add-field-quality-scoring` 和 `git diff --check`。
- [x] 5.4 进行直接代码评审并修复 findings，不使用子 agent。
