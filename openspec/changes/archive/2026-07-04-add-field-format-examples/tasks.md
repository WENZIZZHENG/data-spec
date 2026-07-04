## 1. 规格与测试

- [x] 1.1 校验 OpenSpec proposal/design/spec/tasks 与 P6-55 范围一致。
- [x] 1.2 新增/更新后端测试：字段 API 保存格式约束、AI Context 导出格式对象和 schema、质量评分提示 `format_examples_missing`、变更预览识别格式字段。
- [x] 1.3 新增/更新前端测试：字段库存在格式约束编辑、摘要展示和 JSON 正反例转换入口。

## 2. 后端实现

- [x] 2.1 新增 Flyway 迁移，扩展 `ds_field` 格式约束列并保留旧数据兼容。
- [x] 2.2 扩展 `Field`、`FieldReq`、字段创建/更新/回退和变更预览，保存并校验正反例 JSON 数组。
- [x] 2.3 扩展 AI Context `DATABASE_RULES.md`、`field-catalog.json` 和 `field-catalog.schema.json`，导出 `format` 对象。
- [x] 2.4 扩展字段质量评分，格式敏感字段缺少格式约束或有效样例时给出 `format_examples_missing`。

## 3. 前端实现

- [x] 3.1 更新 OpenAPI schema 与前端类型。
- [x] 3.2 扩展字段库表格和 create/edit dialog，支持维护格式类型、pattern、单位、精度、时区、空值策略、正例、反例和备注。
- [x] 3.3 确保前端以每行一个示例编辑，提交为 JSON array 字符串，读取时可兼容空值或坏格式。

## 4. 文档、验证与收口

- [x] 4.1 更新 README/TODO，记录第一版能力、使用方式和边界。
- [x] 4.2 运行 `openspec validate add-field-format-examples --strict`。
- [x] 4.3 运行后端相关测试、`mvn test`、前端 `pnpm test`、`pnpm build`，并在后端可用时运行 `pnpm gen:api`/`pnpm check:api`。
- [x] 4.4 使用独立代码评审 agent 审查本次变更，修复 findings 后复跑必要验证。
- [x] 4.5 归档 OpenSpec change 并提交。

## Verification Evidence

- `openspec validate add-field-format-examples --strict`：通过。
- `mvn "-Dtest=FieldServiceImplTest,FieldQualityServiceImplTest,AiContextExportServiceTest,StandardChangePreviewServiceImplTest,StandardChangePreviewControllerTest" test`：74 tests, 0 failures, 0 errors。
- `mvn test`：377 tests, 0 failures, 0 errors。
- `pnpm gen:api`：从 `http://localhost:8090/api-docs` 重新生成 `dataspec-web/src/api/schema.ts`。
- `pnpm check:api`：`OpenAPI schema.ts 已是最新`。
- `pnpm test`：99 tests, 0 failures。
- `pnpm build`：通过，保留既有 Rolldown pure annotation 和 chunk size warning。
- 独立代码评审 agent `Pascal`：发现 2 个 findings；已修复空字符串格式样例保真、`formatNullPolicy`/反例样例质量评分判断，并补回归测试。
- 修复后 `mvn "-Dtest=FieldServiceImplTest,FieldQualityServiceImplTest,AiContextExportServiceTest" test`：71 tests, 0 failures, 0 errors。
- 修复后 `mvn test`：378 tests, 0 failures, 0 errors。
- 修复后 `pnpm test`：99 tests, 0 failures。
- 修复后 `pnpm build`：通过，保留既有 Rolldown pure annotation 和 chunk size warning。
- 修复后 `pnpm check:api`：`OpenAPI schema.ts 已是最新`。
