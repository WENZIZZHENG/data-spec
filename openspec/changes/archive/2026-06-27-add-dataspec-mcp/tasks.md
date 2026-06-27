## 1. OpenSpec 与测试

- [x] 1.1 验证 `add-dataspec-mcp` OpenSpec artifacts 通过 `openspec validate`。
- [x] 1.2 新增 MCP 单元测试，并先观察缺失脚本导致的失败。

## 2. MCP 实现

- [x] 2.1 新增 `tools/dataspec-mcp.mjs`，实现启动参数解析和 JSON-RPC stdio handler。
- [x] 2.2 实现 `resources/list` 与 `resources/read`，暴露字段目录、数据库规则和规则 YAML。
- [x] 2.3 实现 `prompts/list` 与 `prompts/get`，提供建表、SQL 评审和字段设计 prompts。
- [x] 2.4 实现 `tools/list` 与 `tools/call`，支持 `lint_sql` 和 `get_field_catalog`。
- [x] 2.5 实现结构化错误输出，区分 lint 业务结果与 MCP/HTTP 调用失败。

## 3. 文档与验证

- [x] 3.1 更新 README 和 TODO 中的 MCP 状态/用法。
- [x] 3.2 运行 Node MCP 单元测试。
- [x] 3.3 运行后端测试和前端构建，确认本轮未破坏已有链路。
- [x] 3.4 运行 OpenSpec validate 和 diff 空白检查。
- [x] 3.5 进行直接代码评审，检查协议方法、资源 URI、工具参数、错误输出和项目选择。
- [x] 3.6 通过验证后提交本功能改动。
