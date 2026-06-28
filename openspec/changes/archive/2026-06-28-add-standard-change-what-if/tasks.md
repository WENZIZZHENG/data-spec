## 1. OpenSpec

- [x] 1.1 创建 P6-41 proposal、design、delta specs 和 tasks。
- [x] 1.2 通过 OpenSpec change 校验。

## 2. 后端

- [x] 2.1 新增标准变更 what-if model/service/controller。
- [x] 2.2 字段更新预览复用字段影响分析并输出 diff、风险、验证命令和回退提示。
- [x] 2.3 规则更新/启停预览输出规则 diff、SQL lint/AI Context 影响和变更日志提示。
- [x] 2.4 增加后端单测覆盖字段、规则和无变化场景。

## 3. 前端

- [x] 3.1 新增前端 API 与类型/显示工具。
- [x] 3.2 字段编辑保存前使用 what-if 预览确认，保留原影响/变更入口。
- [x] 3.3 规则编辑和启停前展示 what-if 摘要。
- [x] 3.4 增加前端测试或 smoke 覆盖新增入口。

## 4. 验证、评审、归档

- [x] 4.1 执行 `mvn test`、`pnpm test`、`pnpm build`、`npx.cmd openspec validate --all` 和 `git diff --check`。
- [x] 4.2 完成本地结构化代码评审并修复 findings。
- [x] 4.3 更新 TODO/README 状态和 Verification Evidence。
- [x] 4.4 创建本地 commit，归档 OpenSpec change 并再次验证。
