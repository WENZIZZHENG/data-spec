## Context

DataSpec 已有字段标准搜索 API、AI Context scope 参数、CLI `search-fields`、MCP `search_fields`、稳定引用和 post-check 结果。当前问题不是缺少单点搜索，而是同一类组合条件在 API、CLI、MCP、AI Context 和前端之间用不同参数表达，AI 无法把一次有效查询稳定复用到另一个入口，也无法解释哪些筛选被应用、降级或忽略。

P6-167 发生在稳定引用之后，适合把 `stableRef`、`canonicalRef`、字段状态、敏感标记和字段搜索摘要统一到一条只读 query expression。该变更影响 API/CLI/MCP/AI 外部协议，按 SDD standard 偏 full 执行；提交前必须独立 agent 评审。

## Goals / Non-Goals

**Goals:**

- 定义 Standard Query DSL v1，用 JSON 表达项目内标准对象查询。
- 第一版执行目标限定为字段标准 `target=FIELD`，复用现有 `FieldService.search` 的排序、命中理由、稳定引用和安全边界。
- 支持 legacy query/category/tag/status/sensitive/sourceBatchId 参数到 DSL 的确定性映射，避免旧客户端破坏。
- 在 API、CLI、MCP 和 AI Context scope 中复用同一 DSL，并输出可解释 `querySummary`、`appliedFilters`、`ignoredFilters`、`resultCount` 和 `nextQueryHints`。
- 用 Schema Registry 和 CLI/MCP fixtures 固化输入、输出和安全元数据。

**Non-Goals:**

- 不做任意 SQL 查询、表达式执行、脚本求值或全文搜索平台。
- 不跨项目查询，不绕过字段可见性、安全红线或敏感字段策略。
- 不在第一版覆盖所有标准对象；规则、枚举、模板、证据可后续扩展同一 DSL。
- 不重做字段搜索排序算法；DSL 是可复用表达层，不替换现有检索质量逻辑。

## Decisions

1. **DSL 使用 JSON AST，不引入字符串解析语法。**
   - 选择：请求中使用 `standardQuery` 对象，包含 `target`、`text`、`filters[]`、`sort[]`、`limit`、`explain` 和 `strict`。
   - 原因：AI、CLI、MCP 和前端都能稳定生成 JSON；JSON Schema 可描述字段语义和安全限制。
   - 替代方案：采用 Sourcegraph 风格字符串语法。暂不采用，因为需要额外 parser、转义规则和错误定位，第一版收益不够。

2. **第一版只执行字段标准查询。**
   - 选择：`target=FIELD` 时复用 `FieldService.search`；`target` 缺失默认 `FIELD`，其它 target 在 strict 模式返回错误，在非 strict 模式返回 ignored filter/unsupported target 诊断。
   - 原因：P6-167 的核心复用场景是字段标准、AI Context 裁剪和建表/SQL 修复；已有实现最完整。
   - 替代方案：一次性覆盖 FIELD/ENUM/RULE/SNAPSHOT。暂不采用，避免在本轮把多个标准对象搜索质量一起重做。

3. **legacy 参数映射为 DSL，再执行统一计划。**
   - 选择：`/api/fields/search` 继续接受 query/category/tag/status/sensitive/sourceBatchId；内部转换成 Standard Query Plan，结果保留旧字段并 additive 输出 querySummary。
   - 原因：旧客户端和前端页面无需立即迁移，新入口和旧入口可以共享行为。
   - 替代方案：新增完全独立 `/api/standard-query/search` 但不改字段搜索。放弃，因为会产生两套搜索语义。

4. **可解释结果按“应用、忽略、提示”三段输出。**
   - 选择：解析结果包含 `normalizedQuery`、`appliedFilters[]`、`ignoredFilters[]`、`resultCount`、`returnedCount`、`truncated` 和 `nextQueryHints[]`。
   - 原因：AI 需要知道是条件真正命中，还是被降级/忽略；用户也需要可读问题定位。
   - 替代方案：只返回匹配字段列表。放弃，因为无法防止 AI 误以为所有条件都生效。

5. **安全边界沿用只读、bounded 和 secret-safe。**
   - 选择：限制 filter 数量、文本长度、limit 上限；所有 user-controlled text、ignored reason、CLI/MCP error 均脱敏；`refs`、`query` 和 `standardQuery` 标记为 sensitive input。
   - 原因：DSL 会进入 AI 工具链，不能成为凭据或业务行泄漏通道。
   - 替代方案：只依赖调用方不要传 secrets。放弃，和现有 AI/CLI/MCP 安全约定冲突。

## Risks / Trade-offs

- **[Risk] DSL 支持字段太少，AI 仍想表达更复杂条件。** → Mitigation：非 strict 模式给出 `ignoredFilters` 和 `nextQueryHints`；strict 模式失败并说明支持字段。
- **[Risk] legacy 参数和 DSL 行为不一致。** → Mitigation：以 DSL plan 为唯一内部表示，补 legacy-to-DSL 和 API/CLI/MCP fixture 测试。
- **[Risk] 复杂 JSON 过滤导致实现膨胀。** → Mitigation：只支持 allowlist field/op；不支持任意嵌套表达式执行，最多支持 `AND` 组合。
- **[Risk] 查询文本含 secret。** → Mitigation：复用 `SensitiveDataSanitizer`，测试覆盖 token/password/Authorization/JDBC/DSN。
- **[Risk] 前端一次性迁移成本高。** → Mitigation：第一版保持现有 UI 控件，先在 API wrapper/type 和 AI Context 参数中支持 DSL。

## Migration Plan

- 不做数据库迁移；DSL 是请求/响应层和只读执行层。
- 新增 API/CLI/MCP 输出为 additive；旧 `search-fields` 和 `/api/fields/search` 参数继续可用。
- 回滚时可移除新增 DSL API/CLI/MCP 入口和 additive 字段，旧搜索仍工作。
- 完成后保留 active change，不自动 archive；用户要求 archive 时再同步主规格。

## Open Questions

- 前端是否需要在本轮增加 DSL 文本/JSON 编辑器？默认不做，只保留现有筛选控件和 API 类型支持。
- `updatedSince` 是否依赖所有字段都有可靠更新时间？实现时若字段表只提供创建/更新时间其中之一，先按已有字段处理，缺失时进入 ignored filter 或提示。
