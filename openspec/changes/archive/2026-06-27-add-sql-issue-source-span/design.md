## Context

当前 lint 规则基于 `TableDef`/`ColumnDef` 运行，问题结构里只有表名和字段名；前端 Monaco 编辑器、CLI JSON、MCP 和 PR Review 都只能展示问题描述，不能跳到具体 SQL 位置。JSqlParser 当前解析模型没有在项目里保留原始 token span，因此本轮优先做稳定、低风险的文本定位回填。

## Goals / Non-Goals

**Goals:**
- 为 `LintIssue` 增加统一定位字段：`line`、`column`、`sourceStart`、`sourceEnd`。
- 在 `/api/lint` 主链路和检查记录详情中保留这些字段。
- 前端 SQL 校验页展示定位信息，并可点击跳转到 Monaco 对应位置。
- 用测试锁定 `examples/bad-example.sql` 或等价 SQL 的定位输出。

**Non-Goals:**
- 不做完整 SQL AST source map。
- 不做 GitHub inline comment。
- 不自动覆盖源 SQL 或执行数据库变更。
- 不保证所有规则都能定位；无法定位时字段允许为空。

## Decisions

### 1. 在 lint 编排层统一回填定位

规则仍只负责输出业务问题和 `tableName/columnName`。`SqlLintService` 在收集所有 issue 后调用 source span resolver，按原始 SQL 文本、表名、字段名回填定位。这样可以避免改动每条规则，也能让已有检查记录序列化逻辑自然保存新字段。

### 2. 第一版使用启发式文本定位

定位策略按优先级处理：

1. 有 `tableName + columnName`：优先定位列定义中的字段名。
2. 只有 `tableName`：定位 `CREATE TABLE <table>` 中的表名。
3. 找不到精确列定义时：回退到 SQL 全文中第一次出现的字段名或表名。
4. 仍找不到时：定位字段保持为空。

字段命名使用 1-based `line/column`，`sourceStart/sourceEnd` 使用 Monaco/JavaScript 友好的 0-based 字符偏移。

### 3. 前端点击行列跳转

SQL 校验页新增“位置”列。存在 `line` 时显示 `line:column` 按钮，点击后调用 `editor.revealPositionInCenter` 和 `editor.setPosition`。历史记录详情只展示位置，不跳转，因为详情里的原 SQL 当前是只读代码块。

## Risks / Trade-offs

- 启发式定位可能在同名字段多次出现时选中第一处定义 → 第一版优先定位列定义行，后续如需要再引入 parser/token source map。
- 引号、schema 前缀、MySQL 反引号会增加匹配复杂度 → resolver 需要同时匹配裸名、双引号和反引号。
- 检查记录中保存新字段会改变 `issuesJson` 结构 → JSON 反序列化对新增字段兼容，旧记录缺字段时前端按空值处理。
