## Context

当前 `SqlIssueSourceSpanResolver` 在 lint 规则执行后，根据 issue 的 `tableName/columnName` 回填 `line/column/sourceStart/sourceEnd`。这个方案没有改动每条规则，风险低，但第一版仍有几个限制：多表 SQL 中同名字段可能被误定位，`CREATE TABLE schema.table`、双引号/反引号和 COMMENT 语句覆盖不充分，CLI `review-pr` 评论也只展示表/字段，不展示可用于文件级 review 的行列范围。

P5-5 需要在现有兼容字段基础上增强定位质量，并让 CLI JSON/Markdown 输出稳定携带文件内位置，为后续 GitHub inline review API 留出数据基础。

## Goals / Non-Goals

**Goals:**

- 为 lint issue 提供更稳定的 source range：起止行列、0-based 偏移和定位类型。
- 定位优先限定在对应 `CREATE TABLE` 语句范围内，覆盖 schema 前缀、引号标识符、MySQL 反引号、方括号和多语句 SQL。
- 对缺注释、命名、类型等常见规则，稳定落到表名或列名所在片段；无法定位时继续允许空定位。
- CLI `lint-files` JSON 和 `review-pr` Markdown 能展示文件路径与 issue 行列范围。
- 补充 fixture/golden 测试，避免后续规则改动破坏定位。

**Non-Goals:**

- 不实现完整 AST token source map。
- 不直接调用 GitHub Pull Request Review inline comment API。
- 不改变现有 `line/column/sourceStart/sourceEnd` 的含义。
- 不为了定位执行或修改源数据库。

## Decisions

1. **保留 `LintIssue` 兼容字段，追加 range 元数据**
   - 新增 `lineEnd`、`columnEnd`、`locationKind` 或等价字段，而不是替换现有字段。
   - 理由：前端、CLI、MCP 和历史检查记录已经消费 `line/column/sourceStart/sourceEnd`；追加字段可向后兼容。
   - 替代方案：新增嵌套 `sourceLocation` 对象；类型更干净，但会增加前端和 OpenAPI 迁移范围。

2. **在 resolver 内构建轻量 SQL 文本索引**
   - 扫描 SQL 中的 `CREATE TABLE` 语句，记录表 token span、表体范围和列定义行。
   - 定位时先按表范围查列，再按 COMMENT ON TABLE/COLUMN 语句查目标，最后才回退全文匹配。
   - 理由：相比完整 parser source map，文本索引实现量小，能解决当前最痛的误定位。

3. **规则继续只输出业务语义**
   - 各 lint rule 仍只设置 `tableName/columnName/ruleCode` 等业务字段，不要求每条规则自己计算 source span。
   - 理由：保持规则简单，减少重复定位逻辑；后续新增规则自动复用 resolver。

4. **CLI inline 基础先落在 JSON 和 Markdown，不落 GitHub API**
   - `lint-files` JSON 继续按 `files[]` 输出，每个 issue 直接携带行列范围；`review-pr` Markdown 显示 `行 x:y-x2:y2`。
   - 理由：GitHub inline comment 需要 diff hunk position，不只是文件行号；第一版先提供稳定原始文件定位，下一步再接 GitHub compare/diff 映射。

## Risks / Trade-offs

- **SQL 文本索引仍可能被复杂 DDL 干扰** → 限定第一版目标为项目已有 PostgreSQL/MySQL DDL 写法，fixture 覆盖真实高频样例；无法定位时保留空值。
- **COMMENT ON 和列定义都能命中时存在选择问题** → 字段级缺注释优先列定义，COMMENT 相关后续可通过 ruleCode/locationKind 调整；测试锁定当前选择。
- **新增定位字段影响 OpenAPI/前端类型** → 提交生成或手写类型更新，并用 `pnpm build` 作为契约门禁。
- **GitHub inline 仍不能直接使用原始行号** → README/TODO 明确这是 inline 基础数据，不声称已实现 inline review API。

## Migration Plan

无需数据库迁移。旧检查记录没有新增字段时，前端和 CLI 按空值处理；新检查记录保存新增 JSON 字段。

## Open Questions

无。第一版以 source range 稳定性和 CLI 文件级输出为交付边界。
