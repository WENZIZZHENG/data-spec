## Why

P4 已经给 SQL lint issue 补了第一版 `line/column/sourceStart/sourceEnd`，足够前端跳转，但仍是按全文启发式查找。P5-5 要把定位提升到可支撑 GitHub inline review 的基础数据层，减少同名字段、schema 前缀、COMMENT 语句和多表 SQL 下的误定位。

## What Changes

- 增强 SQL source span 解析，优先在对应 `CREATE TABLE` 语句范围内定位表名、列名、COMMENT ON TABLE/COLUMN 和规则对应片段。
- 为 lint issue 增加面向文件级 review 的稳定定位元数据，例如 `locationKind`、`lineEnd`、`columnEnd` 或等价结构，保留现有 `line/column/sourceStart/sourceEnd` 兼容字段。
- 增强 CLI `lint-files` JSON 输出和 `review-pr` 评论内容，输出文件路径、issue 行列范围和可被后续 GitHub inline comment 使用的数据。
- 补充 good/bad SQL fixture 或测试资源，覆盖多语句、多表、schema 前缀、双引号/反引号、COMMENT ON 和类型/命名/缺注释规则定位。
- 更新 README/TODO，说明 P5-5 第一版只提供可靠定位数据和 PR 评论基础，不直接调用 GitHub inline review API。

## Capabilities

### New Capabilities

- `sql-inline-review-location`: SQL lint issue 提供更稳定的表/列/注释 source span 和文件级 review 定位数据，供前端、CLI、MCP 和后续 GitHub inline comment 复用。

### Modified Capabilities

无。

## Impact

- 后端 `lint` model、SQL source span resolver、lint service 和检查记录 JSON。
- 后端 parser/lint 测试和 SQL fixture/golden 样例。
- CLI `lint-files` 聚合 JSON、`review-pr` Markdown 评论和相关 node tests。
- 前端类型与 SQL 校验页位置展示可能需要兼容新增定位字段。
- README、TODO 和 OpenSpec change 文档。
