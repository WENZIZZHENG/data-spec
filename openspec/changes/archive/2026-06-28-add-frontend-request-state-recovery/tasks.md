## 1. OpenSpec 与范围确认

- [x] 1.1 创建 P6-39 proposal、design、delta specs 和 tasks。
- [x] 1.2 通过 OpenSpec change 校验。

## 2. 统一状态工具

- [x] 2.1 新增 `useRequestState` 组合函数，支持 loading、error、errorDetail、lastUpdatedAt、run、retry 和 reset。
- [x] 2.2 新增 `StateBlock` / `ProjectRequired` 或等价轻量组件，统一 empty/error/project-required/retry 展示。
- [x] 2.3 补前端工具测试，覆盖成功请求、失败请求、重试、DataSpec error suggestedAction 和项目缺失状态。

## 3. 高频页面迁移

- [x] 3.1 迁移 Dashboard 页面级加载/错误/项目缺失状态，保持现有 demo project 快捷入口。
- [x] 3.2 迁移 AI Batch 列表加载/错误/项目缺失状态，失败后可重试。
- [x] 3.3 迁移 Field Coverage 结果空态/错误态，覆盖 SQL 和数据库模式。
- [x] 3.4 迁移 SQL Lint 检查记录区项目缺失、空数据和失败重试状态。

## 4. 测试、文档与收尾

- [x] 4.1 扩展 frontend smoke，覆盖统一状态工具、项目必选组件和迁移页面重试入口。
- [x] 4.2 更新 README / TODO，说明 P6-39 已完成能力和边界。
- [x] 4.3 执行 `pnpm test`、`pnpm build`、`npx.cmd openspec validate --all` 和 `git diff --check`。
- [x] 4.4 完成本地结构化代码评审并修复 findings。
- [x] 4.5 创建本地 commit，归档 OpenSpec change 并再次验证。
