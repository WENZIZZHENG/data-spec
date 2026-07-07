## Context

`/api/bootstrap/session` 是 AI 新会话第一跳，返回 `availableCapabilities`、推荐命令和风险提示。它当前不是直接暴露 catalog 全量列表，而是通过 `BOOTSTRAP_CAPABILITY_IDS` 汇总一组高频能力，因此新注册到 capability catalog 的 `standard-evidence` 不会自动进入启动包。

## Goals / Non-Goals

**Goals:**

- 让 READY session bootstrap 的 `availableCapabilities` 包含 `standard-evidence`。
- 保持 bootstrap 仍为轻量摘要，只暴露 API surface、writeRisk、项目要求和 nextActions。
- 用定点测试锁定 `standard-evidence` 在启动包中的只读、安全和无 CLI/MCP 声明边界。

**Non-Goals:**

- 不把 session bootstrap 改成 catalog 全量镜像。
- 不修改 `GET /api/standard-evidence` 的响应结构。
- 不新增 CLI/MCP surface，也不改变 bootstrap JSON shape。

## Decisions

1. **继续使用 bootstrap 白名单，只追加 `standard-evidence`。**
   - 备选：直接输出全部 capability catalog。放弃原因是 bootstrap 需要保持第一跳摘要，避免把低频或写入型能力全部推给 AI。

2. **将 `standard-evidence` 追加到当前高频能力白名单末尾。**
   - 原因：它补充 AI 第一跳的证据查询入口，但第一版只读查询单字段证据，不参与任务编排，也不把 bootstrap 扩展成全量 catalog。

3. **测试断言只读 API surface 和空 CLI/MCP。**
   - 原因：bootstrap 是 AI 外部协议摘要，若未来误声明不存在的 CLI/MCP，AI 会按错误入口执行。

## Risks / Trade-offs

- **[Risk] 启动包能力列表变长** → Mitigation：只增加一个高价值只读能力，不展开全量 catalog。
- **[Risk] AI 误以为 bootstrap 已经生成证据包** → Mitigation：README 和 nextActions 明确需要调用 `/api/standard-evidence`，bootstrap 本身不执行聚合。
- **[Risk] 安全边界漂移** → Mitigation：测试锁定 `READ_ONLY`、API-only 和空 CLI/MCP。
