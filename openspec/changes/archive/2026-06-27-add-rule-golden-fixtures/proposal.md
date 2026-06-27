## Why

SQL parser、lint rule、fixedSql 和反向导入已经是 DataSpec 的核心链路。当前测试大多是内联字符串，能验证单点逻辑，但缺少可复用的 fixture/golden 样例库；后续修改规则或解析器时，很难一眼看出真实 SQL 样例的行为差异。

## What Changes

- 新增后端测试资源目录，按 SQL 方言和链路保存高价值 fixture。
- 新增 fixedSql golden 输出，用文件内容断言修正 SQL，而不是只断言包含片段。
- 新增反向导入 metadata fixture，锁定候选字段、缺注释和非标准字段摘要。
- 新增统一后端 fixture/golden 测试类，并接入现有 `mvn test`。
- 更新 README/TODO，说明 P5-7 第一版覆盖范围和边界。

## Capabilities

### New Capabilities

- `core-golden-fixtures`: 核心 SQL、fixedSql 和反向导入样例通过 fixture/golden 测试防回归。

### Modified Capabilities

无。

## Impact

- 新增 `dataspec-server/src/test/resources/fixtures/**` 测试资源。
- 新增或扩展后端测试类；不改生产业务逻辑。
- README、TODO 和 OpenSpec change 文档更新。
