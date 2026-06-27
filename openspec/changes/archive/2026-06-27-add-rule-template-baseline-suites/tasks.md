## 1. 数据模型与后端基线服务

- [x] 1.1 新增 Flyway 迁移 `V13__add_rule_baseline.sql`，创建项目规则基线记录表并兼容 H2 测试库。
- [x] 1.2 新增规则基线模型、内置模板库和导入/导出 JSON DTO，覆盖 `personal_default`、`strict`、`legacy_compatible`。
- [x] 1.3 新增 `RuleBaselineService` 和 repository，支持列表、当前基线、应用、导出、导入，默认不覆盖已有规则。
- [x] 1.4 新增 `/api/rule-baselines` API，并接入项目访问校验和可读错误。
- [x] 1.5 更新项目初始化/演示项目路径，在创建内置标准项目时应用 `personal_default` 基线且保持幂等。
- [x] 1.6 补后端单元测试，覆盖模板有效性、apply overwrite=false/true、导入校验、导出结构和项目初始化默认基线。

## 2. AI Context 与契约

- [x] 2.1 更新 `rules.yaml` 输出，新增 baseline metadata；无基线时输出 `custom/inferred`。
- [x] 2.2 更新 OpenAPI 生成类型或手动补齐 schema.ts 中新增 API/DTO 类型。
- [x] 2.3 更新 AI Context、CLI/MCP 相关测试，确保新增 baseline 字段为向后兼容新增字段。

## 3. 前端规则基线体验

- [x] 3.1 新增前端 `api/ruleBaseline.ts` 和类型导出。
- [x] 3.2 改造 `RuleConfig.vue`，展示当前基线、内置基线选择、应用/覆盖开关、导出和导入 JSON。
- [x] 3.3 补前端 utility 或 smoke 测试，锁定规则基线入口、当前项目边界和导入/导出文案。

## 4. 文档、评审、验证与提交

- [x] 4.1 更新 README 和 TODO，将 P6-23 标记为已完成第一版并说明边界。
- [x] 4.2 运行 `mvn test`、`pnpm test`、`pnpm build`、相关 Node 测试、`npx.cmd openspec validate add-rule-template-baseline-suites` 和 `git diff --check`。
- [x] 4.3 按代码评审清单做直接评审，不使用子 agent；修复发现的问题或记录暂不处理理由。
- [x] 4.4 通过验证后创建本地 commit。
