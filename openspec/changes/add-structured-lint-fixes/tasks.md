## 1. OpenSpec 与测试

- [x] 1.1 验证 `add-structured-lint-fixes` OpenSpec artifacts 通过 `openspec validate`。
- [x] 1.2 扩展 lint 规则测试，覆盖 snake_case、推荐字段名、禁用字段名、必备列、后缀类型建议字段。

## 2. 实现

- [x] 2.1 扩展 `LintIssue` 结构化建议字段。
- [x] 2.2 核心 lint 规则写入结构化修复建议。
- [x] 2.3 更新 OpenAPI TypeScript schema 和 SQL 校验页展示。
- [x] 2.4 更新 README/TODO。

## 3. 验证

- [x] 3.1 运行后端测试、前端构建、CLI/MCP 测试、OpenSpec validate 和 diff 空白检查。
- [x] 3.2 进行直接代码评审，检查新增字段兼容性、建议语义和无自动改写边界。
- [x] 3.3 通过验证后提交本功能改动。
