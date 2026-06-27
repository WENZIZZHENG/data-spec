## 1. OpenSpec 与测试

- [x] 1.1 验证 `add-ddl-generator-tool` OpenSpec artifacts 通过 `openspec validate`。
- [x] 1.2 新增 DDL 生成 service/controller 测试。
- [x] 1.3 新增 CLI/MCP 测试，覆盖 `generate-ddl` 和 `generate_table_ddl`。

## 2. 实现

- [x] 2.1 后端新增 DDL 生成结果模型、service 和 `/api/generator/ddl/preview`。
- [x] 2.2 CLI 新增 `generate-ddl` 命令。
- [x] 2.3 MCP 新增 `generate_table_ddl` tool。
- [x] 2.4 更新 OpenAPI TypeScript schema 和前端 API wrapper。
- [x] 2.5 更新 README/TODO。

## 3. 验证

- [x] 3.1 运行后端测试、前端构建、CLI/MCP 测试、OpenSpec validate 和 diff 空白检查。
- [x] 3.2 进行直接代码评审，检查 SQL 字符串转义、项目边界、自检结果和不执行数据库变更边界。
- [x] 3.3 通过验证后提交本功能改动。
