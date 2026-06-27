## 1. OpenSpec

- [x] 1.1 创建 P6-17 OpenSpec change，并通过 `openspec validate add-frontend-smoke-regression-gate`。

## 2. 前端冒烟测试

- [x] 2.1 新增前端关键流程冒烟测试，覆盖路由、项目 store、SQL 校验、反向导入、字段库、DDL 生成和 AI Context 关键耦合点。
- [x] 2.2 将冒烟测试接入 `dataspec-web` 现有 `pnpm test`，不新增重量级浏览器依赖。

## 3. 文档与待办

- [x] 3.1 更新 README，说明前端冒烟门禁命令、覆盖范围和边界。
- [x] 3.2 更新 TODO，将 P6-17 标记为已完成第一版并推进下一步顺序。

## 4. 验证、评审与提交

- [x] 4.1 运行 `pnpm test`、`pnpm build`、OpenSpec validate 和 `git diff --check`。
- [x] 4.2 进行直接代码评审，不使用子 agent；修复 findings 或记录暂不处理理由。
- [x] 4.3 创建本地 commit 后继续下一个待办。
