## 1. OpenSpec And Backend Contract

- [x] 1.1 编写并校验 ai-session-bootstrap、CLI、MCP、capability catalog 的 OpenSpec delta。
- [x] 1.2 先添加后端 service/controller 失败测试，覆盖字段完整、无 projectId、无版本快照和 secret 不泄漏。
- [x] 1.3 实现后端 bootstrap DTO、service、controller，并复用 capability catalog 与标准快照。

## 2. CLI And MCP Entry

- [x] 2.1 先添加 CLI `bootstrap` 失败测试，覆盖远端成功、服务不可达 fallback 和 token 不落 stdout。
- [x] 2.2 实现 CLI `bootstrap` 命令、文本/JSON输出和 help 文档。
- [x] 2.3 先添加 MCP resource/tool 失败测试，覆盖 URI、tool 调用、structuredContent 和后端错误。
- [x] 2.4 实现 MCP `session-bootstrap` resource 和 `get_session_bootstrap` tool。

## 3. Contracts And Documentation

- [x] 3.1 重新生成 OpenAPI TypeScript schema，并运行 schema drift 检查。
- [x] 3.2 更新 README 与业务仓库 `.dataspec/README.md` 引用说明，补充 AI 新会话第一条命令。
- [x] 3.3 更新 TODO P6-61 状态和顶部下一步。

## 4. Review And Release

- [x] 4.1 运行后端、前端、OpenSpec、CLI/MCP 和 diff 验证。
- [x] 4.2 使用独立 agent 做代码评审，并修复或记录 findings。
- [x] 4.3 补充 Verification Evidence，归档 OpenSpec，最终验证后创建本地 commit。
