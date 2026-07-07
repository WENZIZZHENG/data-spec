## Context

DataSpec 已有标准字段库、模板、代码集引用、标准使用示例库、DDL preview、字段推荐、Prompt 模板评测和 core golden fixtures。P6-92 的缺口不是新增真实业务数据，而是把这些标准素材组合成可复用、可验证、可被 AI 消费的合成场景样例包。

本次属于 SDD standard：会新增只读后端 API 与 CLI JSON contract，影响 AI/CI 可观察输出；但不改数据库 schema、不写持久化数据、不改变既有 lint/DDL/字段推荐语义。

## Goals / Non-Goals

**Goals:**
- 新增只读合成样例生成 API：`GET /api/synthetic-examples/generate`。
- 新增 CLI：`synthetic-examples generate --project <id> --scenario <user|order|payment|audit> --format json`。
- 输出稳定 JSON 包，包含 `kind`、`schemaVersion`、`projectId`、`scenario`、`specHash`、`generationParams`、good/bad SQL、DDL preview 输入、字段推荐问题、标准问答案例、预期诊断、`safety` 和 `nextActions`。
- 生成器读取项目标准字段和模板摘要；当项目素材不足时使用内置场景骨架补齐，并在 diagnostics/sourceSummary 中说明 fallback。
- 将输出 contract 接入后端测试、CLI 测试和 CLI/MCP fixture 校验。

**Non-Goals:**
- 不调用外部 LLM，不自动造真实业务行数据。
- 不把生成结果直接写入标准使用示例库、数据库或业务仓库。
- 不新增数据库表、迁移、权限模型或后台任务。
- 不实现前端完整页面；第一版仅保证后端/CLI/fixture 可接入前端 smoke 或 Prompt 评测。

## Decisions

1. **生成器作为独立 `syntheticexample` 只读模块，而不是塞进 DDL generator。**
   - 选择：新增 controller/service/model，依赖 `FieldService` 和 `TemplateService` 读取摘要。
   - 原因：输出同时覆盖 SQL、DDL preview、字段推荐和 QA，不是单一 DDL preview；独立模块边界更清晰。
   - 备选：复用 `/api/generator/ddl/preview`。放弃原因是会让 DDL 生成器承担 Prompt/QA/diagnostics 契约，后续维护成本高。

2. **`specHash` 基于规范输入摘要，而不是基于当前时间或随机数。**
   - 选择：对 `schemaVersion`、`projectId`、`scenario`、生成参数、选中的字段/模板摘要和内置场景版本做稳定 JSON hash。
   - 原因：新规则或字段变化后 hash 会变化；相同输入重复运行结果一致，适合 golden fixture 和 AI replay。
   - 备选：使用 UUID 或时间戳。放弃原因是无法作为 fixture 和回归验证依据。

3. **生成“样例包”，不直接持久化。**
   - 选择：返回可复制/可导入草案，例如 `standardQaCases` 与 `goodSql/badSql`，并在 `nextActions` 中提示人工审核后再采纳。
   - 原因：TODO 明确不替代人工维护真实样例；只读包也避免误写项目标准库。
   - 备选：一键写入 usage examples。放弃原因是会引入幂等、审核、撤销和权限问题，超出第一版边界。

4. **场景枚举先收敛为 `user`、`order`、`payment`、`audit`。**
   - 选择：支持四类高频业务对象，每个场景产生有限数量的结构化案例。
   - 原因：覆盖 P6-92 验收，同时便于后续追加场景时做兼容性扩展。
   - 备选：允许任意自然语言场景。放弃原因是没有外部 LLM 时质量不可控，也难以稳定测试。

5. **CLI 只透传后端 JSON，text 输出只做摘要。**
   - 选择：JSON 保留完整 contract；text 输出列出场景、hash、case 计数和下一步动作。
   - 原因：AI/CI 消费优先 JSON；人读摘要不能成为稳定机器契约。

## Risks / Trade-offs

- **项目字段或模板太少导致样例空洞** → 使用内置场景骨架补齐，并返回 diagnostics 标注 fallback 和建议补充标准字段。
- **内置场景和真实行业标准不一致** → `sourceSummary` 明确来源，生成包只作为草案，不自动写入。
- **输出字段过多导致契约漂移风险** → 后端测试与 CLI/MCP fixture 校验稳定字段，新增字段只允许兼容性追加。
- **只读 API 名称仍包含 generate** → `safety` 明确 `readOnly=true`、`writesProject=false`、`containsRealBusinessRows=false`，CLI fixture 也覆盖该边界。

## Migration Plan

- 新增 API/CLI 均为兼容性新增；现有 DDL、字段推荐、usage examples、AI Context 输出不改变默认行为。
- 第一版完成后保留 OpenSpec active，不自动 archive。
- 回滚方式为移除新 controller/service/model、CLI 命令、fixture 和 spec delta；不涉及数据迁移或持久化回滚。
