## 1. OpenSpec 与测试

- [x] 1.1 验证 `add-field-suggestion` OpenSpec artifacts 通过 `openspec validate`。
- [x] 1.2 新增字段推荐 service 测试，覆盖别名/中文描述命中、停用字段过滤、未知描述推荐字段名。
- [x] 1.3 新增 CLI/MCP 测试，覆盖 `suggest-field` 和 `suggest_fields`。

## 2. 实现

- [x] 2.1 后端新增字段推荐模型、service 方法和 `/api/fields/suggest`。
- [x] 2.2 前端 API wrapper 与 OpenAPI TypeScript schema 同步新增推荐类型。
- [x] 2.3 CLI 新增 `suggest-field` 命令。
- [x] 2.4 MCP 新增 `suggest_fields` tool。
- [x] 2.5 更新 README/TODO。

## 3. 验证

- [x] 3.1 运行后端测试、前端构建、CLI/MCP 测试、OpenSpec validate 和 diff 空白检查。
- [x] 3.2 进行直接代码评审，检查评分稳定性、输出契约、CLI/MCP 参数和边界。
- [x] 3.3 通过验证后提交本功能改动。
