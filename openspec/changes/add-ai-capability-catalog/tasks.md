## 1. OpenSpec 与契约基线

- [x] 1.1 校验 OpenSpec change，确认 proposal/design/specs/tasks 格式有效。
- [x] 1.2 梳理现有 API/CLI/MCP/AI Context 稳定入口，确定第一版 capability id 清单。

## 2. 后端能力清单

- [x] 2.1 新增 capability catalog DTO、内置 registry 和只读 service。
- [x] 2.2 新增 `/api/capabilities` 和 `/api/capabilities/{id}`，支持可选 `projectId` 诊断。
- [x] 2.3 保证 catalog 不执行任务、不泄露 token/password/JDBC URL/业务数据行，并标记 writeRisk/preflightChecks。
- [x] 2.4 补后端 service/controller 测试，覆盖 list/show/unknown id/project diagnostics。

## 3. CLI、MCP 与 AI Context

- [x] 3.1 重新生成 OpenAPI TS 类型，并在前端 types 中导出 capability 类型。
- [x] 3.2 CLI 新增 `capability list/show/check`，输出 text/json 并复用现有 DataSpecError 诊断。
- [x] 3.3 MCP 新增 `capability-catalog` resource，支持项目级 catalog 和错误诊断。
- [x] 3.4 AI Context zip 和离线 cache 增加 `.dataspec/capabilities.json`，manifest/README/AGENTS 说明读取顺序。

## 4. 测试、文档与收口

- [x] 4.1 补 Node 测试覆盖 CLI capability 命令、MCP resource 和 AI Context cache artifact。
- [x] 4.2 更新 README、TODO 和必要契约文档，说明能力清单边界。
- [x] 4.3 运行 OpenSpec validate、后端测试、前端测试/build、CLI/MCP Node 测试和 diff 检查。
- [x] 4.4 执行本地结构化代码评审并修复 findings。
- [ ] 4.5 完成提交并归档 OpenSpec change。
