## Context

DataSpec 已有三块可复用基础：字段格式约束和 valid/invalid examples 存在于字段模型，`synthetic-standard-examples` 已能生成 SQL/DDL/问答类 fixture，`cli-mcp-contract-fixtures` 已能校验部分 CLI/MCP descriptor 与安全 metadata。当前缺口是 mock/seed/边界用例仍分散，消费端兼容检查也只覆盖 CLI/MCP 局部 fixture，无法作为“标准变化后自有消费端是否还能解析”的统一门禁。

本变更覆盖 P6-185 和 P6-176，影响 API、CLI、MCP、Schema Registry、tools 和测试。它涉及外部 AI/CLI/MCP 协议、安全样例输出和跨模块契约，按 SDD full 执行；实现、commit 或归档前必须运行匹配验证并启动独立子 agent 评审。

## Goals / Non-Goals

**Goals:**

- 提供项目级只读测试数据包生成能力，输出稳定的 `kind`、`schemaVersion`、`projectId`、`specHash`、`testDataCases`、`seedProfiles`、`mockPayloads`、`coverageReport`、`safety` 和 `nextActions`。
- 支持字段级和轻量对象级用例，覆盖 valid、invalid、boundary 三类 case，以及 JSON、CSV、SQL seed/mock 草稿。
- 复用字段格式约束、枚举生命周期、字段语义、知识卡和表模板基础；元数据不足时使用内置合成 fallback，并在 coverage/diagnostics 中明确标记。
- 提供统一消费端兼容套件，第一版覆盖 DataSpec 自有 API schema registry、AI Context payload、CLI JSON、MCP descriptor/resource/tool 和 contract fixture。
- 提供本地 `consumer-compat check` CLI/tools 入口，输出 breaking/compatible/deprecated 结果、adapter results、迁移提示和可复用验证命令。

**Non-Goals:**

- 不采集、不抽样、不回填真实业务数据行。
- 不自动写入业务数据库、业务仓库、mock server 或前端 fixture 目录。
- 不引入外部 faker、外部 LLM、远程 schema registry 或第三方认证服务作为第一版依赖。
- 不覆盖所有业务规则、所有协议或所有第三方工具；第一版聚焦 DataSpec 自有消费端和示例 adapter。
- 不把兼容套件变成发布审批流；它是本地/CI 可复用门禁和 AI 可读诊断。

## Decisions

1. **测试数据包从标准元数据生成，不从真实数据库采样。**
   - 选择：读取项目字段、枚举、格式约束、字段语义和表模板，结合内置 deterministic generator 生成样例值。
   - 原因：P6-185 的核心安全边界是“不能从真实业务数据里复制样例”；标准元数据已足够生成第一版字段级用例。
   - 替代方案：从连接库采样后脱敏。暂不采用，因为会触碰凭据、真实数据行和隐私边界，需单独 SDD full。

2. **包结构面向 AI 和测试复用，而不是直接执行。**
   - 选择：输出 JSON 为稳定主契约，CSV 和 SQL seed 作为 package 内的文本草稿字段，并标记 `executable=false` 或 `requiresReview=true`。
   - 原因：不同业务库方言和业务约束差异大，直接生成可执行 SQL 容易造成误写入风险。
   - 替代方案：按 PostgreSQL/MySQL 生成可执行 seed。暂不采用，第一版先保守提供可审查草稿。

3. **兼容套件以 checked-in fixtures + 本地 checker 为核心。**
   - 选择：新增 `tools/fixtures/consumer-compatibility-suite.json` 或等价 fixture，checker 校验 required contracts、stable fields、example redaction、adapter coverage 和 breaking rules。
   - 原因：现有 CLI/MCP fixture check 已证明这种模式轻量、稳定、适合 AI/CI 读取。
   - 替代方案：启动后端并动态跑全 API compatibility test。保留为后续增强，因为它会提高验证成本并依赖运行环境。

4. **后端 API 和 CLI/MCP 均为只读入口。**
   - 选择：新增 API 使用 `POST /api/test-data/package/generate` 或项目既有相似路由风格；CLI `test-data generate` 调用后端；MCP 暴露只读工具；`consumer-compat check` 默认本地读取 fixtures，不要求服务可用。
   - 原因：测试数据包需要基于当前项目标准，后端最容易复用 service；消费端兼容检查要能在契约修改后离线先验检查，同时不改变既有 `compat check` 版本握手语义。
   - 替代方案：全部做成本地 CLI。暂不采用，因为本地 CLI 无法稳定读取服务端字段/枚举/模板最新状态。

5. **安全输出走统一脱敏与 bounded 规则。**
   - 选择：所有用户输入、字段示例、诊断、nextActions、fixture examples、stdout/stderr、MCP structuredContent 都经过敏感信息扫描；限制字段数、case 数、payload 文本长度。
   - 原因：测试数据和契约 fixture 会被提交、复制给 AI 和写入日志，必须 safe-to-share。
   - 替代方案：只在 fixture check 做脱敏。放弃，因为 API/CLI/MCP 的运行时输出同样可能带入敏感文本。

## Risks / Trade-offs

- **[Risk] 生成样例看起来像真实数据。** -> Mitigation：显式 `safety.containsRealBusinessRows=false`、`source=SYNTHETIC`、保留 deterministic marker，敏感字段使用占位或不可逆假值。
- **[Risk] 业务规则覆盖不足。** -> Mitigation：coverageReport 标记 `FIELD_ONLY`、`OBJECT_LIGHTWEIGHT`、`missingConstraints` 和 `requiresBusinessReview`，不假装覆盖完整业务规则。
- **[Risk] compat check 与真实 API 运行形状漂移。** -> Mitigation：Schema Registry、CLI/MCP fixture checker 和目标测试同时覆盖；OpenAPI drift 仍走现有门禁。
- **[Risk] 新命令增加 CLI/MCP 维护面。** -> Mitigation：复用现有 command parser、DataSpecError、fixture check 和安全 metadata 模式，不引入新框架。
- **[Risk] fixture 过大影响维护。** -> Mitigation：只收录最小 golden payload 和 stable field paths，大样例放在测试资源或 generated package 单测中。

## Migration Plan

- 不新增数据库表或迁移；测试数据包和兼容套件均为只读计算与 checked-in fixture。
- 新增 API/CLI/MCP 输出为 additive；不删除或重命名现有 synthetic examples、contract fixtures 或 CLI/MCP 入口。
- 回滚时可移除新增 API/CLI/MCP 入口、fixture 和 specs，现有字段标准、合成示例、Schema Registry 和 CLI/MCP 功能不受影响。
- 完成后按项目约定保留 OpenSpec change 为 active，不自动 archive；用户要求 archive 时再同步主规格。

## Open Questions

- 第一版是否增加前端页面：默认不做独立页面，只补 API 类型 / smoke wiring；如实现中发现已有 Schema Registry 页面能低成本展示，再做最小展示。
- SQL seed 草稿是否按多方言输出：默认输出 ANSI-ish 草稿和 dialect note，不承诺可直接执行。
