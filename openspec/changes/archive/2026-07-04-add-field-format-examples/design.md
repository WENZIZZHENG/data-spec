## Context

标准字段已经有 `dataType`、`exampleValue`、`sensitive`、`codeSetId`、生命周期状态、质量评分和 AI Context 导出。P6-55 要补的是“值形态”层：字段如何取值、单位是什么、正反例有哪些、时间是否带时区、JSON 是否有结构说明。

## Goals / Non-Goals

**Goals:**
- 在标准字段上保存可选格式约束，支持 AI 和前端读取。
- 格式约束进入 AI Context 和 create-table/fix-sql 相关上下文。
- 质量评分能识别格式敏感字段缺少格式样例。
- 前端字段库能维护这些信息。

**Non-Goals:**
- 不扫描真实业务数据行。
- 不执行正则、JSON Schema 或数据质量任务。
- 不强制所有字段配置正则或样例。
- 不拆出复杂版本化格式模板库。

## Decisions

1. **使用 `ds_field` 轻量扩展，而不是新建格式约束表。**
   - 原因：第一版约束与字段生命周期一致，读写路径已经围绕 `Field` 实体；扩展列能最小接入字段库、快照、备份、AI Context 和 OpenAPI。

2. **正反例用 JSON array 字符串存储，导出时转为数组。**
   - 原因：PostgreSQL/H2/MyBatis 现有字段模型以普通列为主；字符串存储兼容迁移和 OpenAPI，AI Context 仍输出稳定数组结构。

3. **`formatPrecision` 使用字符串。**
   - 原因：不同格式的精度含义不同，如金额 `scale=2`、时间 `millisecond`、经纬度 `6dp`；字符串比单个整数更少误导。

4. **新增 `formatNullPolicy`，但不改变现有 `nullable` 语义。**
   - 原因：`nullable` 是数据库可空约束，`formatNullPolicy` 仅描述值层策略，如 `empty_string_as_null` 或 `not_applicable`，避免把业务含义塞进注释。

5. **质量评分只做提示，不阻断保存。**
   - 原因：个人/小团队阶段优先帮助 AI 补上下文，不引入企业化审批或强制治理。

## Risks / Trade-offs

- [Risk] JSON 字符串可能被手工写坏。→ Mitigation：后端保存时校验正反例 JSON 必须是字符串数组，前端用每行一个示例生成 JSON。
- [Risk] 格式类型无法一次覆盖所有行业。→ Mitigation：`formatType` 允许自由文本/可创建选项，第一版提供常见选项。
- [Risk] 质量评分过度提示。→ Mitigation：只对命名、显示名、注释、分类、标签中命中金额/联系方式/时间/JSON/状态等关键词的字段触发。

## Verification Strategy

- OpenSpec：`openspec validate add-field-format-examples --strict`。
- 后端：优先跑 `mvn -Dtest=AiContextExportServiceTest,FieldQualityServiceImplTest,FieldControllerTest,StandardChangePreviewServiceImplTest test`，收尾跑 `mvn test`。
- 前端：更新 schema 后跑 `pnpm test` 和 `pnpm build`。
- 契约：后端启动后执行 `pnpm gen:api` 和 `pnpm check:api`，确认 `schema.ts` 与 OpenAPI 对齐。
