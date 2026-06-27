## Context

当前后端已经有 parser、lint rule、fixedSql 和反向导入单测，但多数 SQL 样例内联在测试方法里。这样适合小规则单测，却不利于维护一批真实 SQL 场景：一旦 parser 或规则调整，测试失败只能看到断言片段，很难直接比较输入 SQL 与期望输出。

P5-7 需要把核心链路的高价值样例沉淀为文件化 fixture/golden，并继续通过现有 `mvn test` 入口执行。

## Goals / Non-Goals

**Goals:**

- 建立 `src/test/resources/fixtures` 测试资源目录。
- 覆盖 PostgreSQL good/bad SQL、MySQL DDL、fixedSql golden 和反向导入 metadata 样例。
- 让 fixedSql 使用完整 golden 文件断言，便于 review 看到预期 SQL 的变化。
- 让反向导入测试读取 JSON fixture，锁定 candidate/missing/non-standard 统计。
- 所有新增测试接入 `mvn test`，不新增单独脚本作为唯一入口。

**Non-Goals:**

- 不追求完整 SQL 方言覆盖。
- 不新增生产代码逻辑或数据库迁移。
- 不把所有已有内联测试迁移为 fixture；第一版只覆盖高价值样例。

## Decisions

1. **fixture 目录按链路组织**
   - `fixtures/sql/postgresql-good.sql`
   - `fixtures/sql/mysql-bad.sql`
   - `fixtures/sql/fixed-sql-input.sql`
   - `fixtures/golden/fixed-sql-user-order.sql`
   - `fixtures/reverseimport/database-metadata.json`
   - 理由：比按测试类散落更容易给后续样例扩展，也方便 review 文件差异。

2. **新增一个聚合测试类**
   - 新增 `CoreGoldenFixturesTest` 读取资源文件，复用现有 `SqlParserService`、`SqlLintService`、`FixedSqlGenerator` 和 `ReverseImportServiceImpl`。
   - 理由：P5-7 目标是建立 fixture/golden 基线，不改各规则单测职责。

3. **golden 断言做文本归一化**
   - 读取 expected SQL 后统一换行符和首尾空白。
   - 理由：Windows/Unix 行尾差异不应导致 golden 测试误报。

4. **JSON fixture 使用 Jackson 解析**
   - 反向导入 metadata 样例用结构化 JSON 读取为 `FieldCandidate`，避免测试里手写长对象列表。
   - 理由：符合项目现有 ObjectMapper 使用方式，也更贴近前端/数据库直连返回结构。

## Risks / Trade-offs

- **golden 文件过大难维护** → 第一版只保留短 SQL，锁定关键行为。
- **聚合测试失败定位变粗** → 每个 fixture 场景拆成独立测试方法。
- **fixture 与已有内联测试重复** → 接受少量重复，换取文件化回归样例的可读性。
