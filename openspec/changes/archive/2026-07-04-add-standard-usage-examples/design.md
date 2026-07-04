## Context

DataSpec 已有字段 `exampleValue`、字段值格式正/反例、SQL good/bad fixture、Prompt 模板和 scoped AI Context，但它们覆盖的是“值长什么样”或“整段 SQL 是否合规”。P6-60 需要补齐更细粒度的“字段/规则/模板如何正确使用，以及哪些历史写法不要模仿”，并让 AI Context 能按任务裁剪。

该能力会同时影响数据模型、API、AI Context 导出和前端入口，因此采用 OpenSpec-first，并延续现有 Spring Boot + MyBatis Plus + Vue 3 + OpenAPI 类型生成方式。

## Goals / Non-Goals

**Goals:**
- 提供项目级结构化示例库，支持字段、规则、模板三类 scope 的正例和反例。
- 支持 API/前端维护示例，至少覆盖列表、新增、编辑、删除或启停。
- AI Context 导出少量高价值示例，按 scope/query/limit 裁剪，保留反例原因和使用建议。
- 保证示例内容不采样真实业务数据，不导出 token、密码、JDBC URL 等敏感字符串。
- 用后端测试和前端冒烟测试锁定输出格式。

**Non-Goals:**
- 不做企业审批、发布流、权限审批或多人协同治理。
- 不把示例库扩展成完整教程、课程或大文档系统。
- 不从真实数据库行采样生成示例。
- 不引入外部 LLM 或自动评估模型效果。

## Decisions

1. 新建 `ds_standard_usage_example` 表，而不是扩展 `ds_field`。
   - 原因：反例可能绑定规则或模板，字段值格式例子已经由 `ds_field.valid_examples_json/invalid_examples_json` 覆盖；独立表能表达 cross-field 的 bad pattern。
   - 备选：把所有例子塞入字段 JSON。放弃原因是规则/模板 scope 会变成不可查询的自由文本。

2. scope 使用稳定字符串：`FIELD`、`RULE`、`TEMPLATE`、`GENERAL`。
   - 原因：兼容前端选择、OpenAPI 类型和后续 CLI/MCP 使用；`GENERAL` 用于跨字段通用示例。
   - 备选：拆多张表。放弃原因是第一版维护成本高，且字段结构高度一致。

3. 示例类型使用 `GOOD` / `BAD`，状态使用 `enabled` / `disabled`。
   - 原因：直接对应 AI 需要学习的正例/反例；状态沿用字段标准的轻量状态语义。

4. AI Context 同时输出 `.dataspec/usage-examples.json`，并在 `field-catalog.json` 中追加 `usageExamples`。
   - 原因：单独文件便于 agent 直接读取；追加到 field catalog 便于 MCP/search 入口复用同一 JSON。
   - 兼容性：字段目录现有 top-level 字段保持不变，新字段为 additive。

5. AI Context 示例裁剪以启用状态、高优先级、scope/query 匹配和上限为核心。
   - 原因：AI Context 需要小而准，不能把示例库变成噪声源。
   - 边界：第一版不做复杂语义召回，只用字段名、displayName、comment、tags、scope、ruleCode、templateId 和示例文本做轻量匹配。

6. 内容脱敏采用现有敏感文本边界思想：服务端保存和导出前拒绝明显 secret/JDBC URL。
   - 原因：P6-60 明确不采样真实业务数据，不导出敏感值；拒绝比静默改写更容易让个人用户修正示例。

## Risks / Trade-offs

- [Risk] 示例文本字段自由度高，可能误填真实手机号、token 或 JDBC URL。→ Mitigation：保存时做明显 secret/JDBC URL 拦截，README 和页面文案强调使用脱敏示例。
- [Risk] AI Context 包体继续膨胀。→ Mitigation：默认只导出高 priority、启用状态和裁剪后的少量示例，并在 metadata 中记录 truncation。
- [Risk] 多 scope 第一版查询不够智能。→ Mitigation：保持结果结构稳定，后续可替换检索策略而不破坏 API。
- [Risk] 前端页面过重影响现有标准维护流程。→ Mitigation：作为独立任务入口接入，不改字段库主表交互。

## Migration Plan

- 新增 Flyway 迁移创建 `ds_standard_usage_example` 和必要索引。
- 部署后老项目默认没有示例，现有 API 和 AI Context 继续工作。
- 回滚时删除新表和前端入口即可；已有字段、规则和模板数据不受影响。
