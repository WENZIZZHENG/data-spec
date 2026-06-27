## Why

DataSpec 前端已经覆盖字段库、SQL 校验、DDL、反向导入、AI Context、工作台等高频页面，但当前主要靠 `pnpm build` 和局部工具函数测试兜底。项目选择、路由入口、核心 API 封装和关键按钮一旦漂移，AI 或用户可能到页面上才发现流程断了。

## What Changes

- 新增前端关键流程冒烟回归门禁，覆盖项目切换、SQL 校验记录/fixedSql、反向导入预览、字段库筛选、DDL 预览和 AI Context 导出等页面链路的静态/组件集成约束。
- 将冒烟测试接入现有 `pnpm test`，继续沿用 `node --test`，不引入重量级浏览器测试平台。
- 更新 README 和 TODO，说明 P6-17 第一版能力、验证命令和边界。

## Capabilities

### New Capabilities

- `frontend-smoke-regression-gate`: 覆盖前端关键页面、路由、项目状态和 API 调用耦合关系的轻量冒烟门禁。

### Modified Capabilities

- 无。

## Impact

- 前端：新增测试或测试辅助，更新 `package.json` 测试入口。
- 文档：README 增加前端冒烟门禁说明，TODO 将 P6-17 标记为已完成第一版并推进下一步顺序。
- OpenSpec：新增本 change 的 proposal/design/spec/tasks。
