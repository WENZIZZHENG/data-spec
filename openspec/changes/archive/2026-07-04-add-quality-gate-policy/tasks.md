## 1. 数据模型与契约

- [x] 1.1 校验 proposal/design/spec/tasks 与 P6-59 范围一致，确认不做审批流和默认阻断。
- [x] 1.2 新增 Flyway 迁移 `ds_standard_quality_gate`，包含项目唯一索引、阈值字段、扩展配置和注释。
- [x] 1.3 新增后端 entity/mapper/repository/model DTO，并确保 OpenAPI 可见。

## 2. 后端质量门禁

- [x] 2.1 新增 quality gate service，支持读取默认配置、保存配置和阈值校验。
- [x] 2.2 实现评估接口，复用字段质量评分、最近标准健康快照或请求输入的覆盖/lint summary。
- [x] 2.3 输出 `PASS/FAIL/DISABLED`、check results、failedChecks、summary、nextActions 和脱敏诊断。
- [x] 2.4 新增 controller API：读取配置、保存配置、执行评估，并做 projectId 边界校验。

## 3. CLI 与 AI 消费

- [x] 3.1 CLI 新增 `quality-gate check --project <id> --format json`，PASS=0、FAIL=1、错误=2。
- [x] 3.2 CLI 输出复用现有 DataSpecError 诊断，确保不泄露 token、password、Authorization 或完整 JDBC URL。
- [x] 3.3 更新 CLI 契约测试，覆盖 PASS、FAIL 和服务错误。

## 4. 前端入口

- [x] 4.1 重新生成 OpenAPI 类型并新增前端 quality gate API 封装。
- [x] 4.2 在标准健康或字段质量入口展示门禁状态、失败项、阈值/实际值和 nextActions。
- [x] 4.3 无项目时不调用项目级 API，失败项可跳转已有修复页面。
- [x] 4.4 更新前端 smoke 测试覆盖 quality gate 页面/API 耦合。

## 5. 文档、验证与收口

- [x] 5.1 更新 README/TODO，记录 P6-59 第一版能力、边界和 CLI 用法。
- [x] 5.2 新增/更新后端测试覆盖配置保存、阈值校验、PASS/FAIL/SKIPPED、敏感信息边界。
- [x] 5.3 运行必要验证：OpenSpec strict、后端相关测试、`mvn test`、`pnpm gen:api`、`pnpm check:api`、`pnpm test`、`pnpm build`、`node --test`、`openspec validate --all`。
- [x] 5.4 使用独立代码评审 agent 审查本次变更，修复 findings 后复跑必要验证。
- [x] 5.5 补 `Verification Evidence`，归档 OpenSpec change 并提交。
