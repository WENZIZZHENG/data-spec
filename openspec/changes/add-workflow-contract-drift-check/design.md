## Context

DataSpec 的 workflow recipe 清单由 `tools/dataspec-workflows.mjs` 维护，并被 CLI、MCP、AI Context 和 task card 复用。最近新增 `standard-evidence-review` 时，README、`docs/ai-contracts.md`、`TODO.md`、MCP schema description 和后端 AI Context 需要多处同步，说明 recipe id 清单已经成为 AI 可依赖契约的一部分。

现有 `tools/dataspec-status-check.mjs` 已用于本地确定性状态检查，覆盖 TODO 队列、OpenSpec 状态、README 工具入口和 Markdown 链接。把 workflow recipe 文档漂移检查放入这个入口，能让后续 agent 在提交前通过同一命令发现清单不同步。

## Goals / Non-Goals

**Goals:**

- 以 `supportedWorkflowRecipeIds()` 作为唯一 canonical recipe id 来源。
- 检查 `docs/ai-contracts.md` 中 `.dataspec/workflows.md` 稳定字段说明是否包含所有当前 recipe id。
- 检查 `TODO.md` 中任务卡已完成能力说明是否包含所有当前 recipe id，避免 P6-62 类状态描述再次滞后。
- 输出稳定 issue code、file、line 和修复建议，便于 AI agent 自动定位。
- 保持 CLI 本地只读，不联网、不访问 DataSpec 服务、不执行 workflow。

**Non-Goals:**

- 不改变 workflow recipe JSON/Markdown 输出结构。
- 不新增后端 API、MCP resource 或 task card schema。
- 不解析任意自然语言里的完整 recipe 语义，只检查 recipe id 是否同步出现。

## Decisions

1. **复用 status-check，而不是新增脚本。**
   - 原因：README 已把 status-check 作为文档/OpenSpec 漂移检查入口，新增同类脚本会增加提交前验证成本。
   - 备选方案：扩展 `dataspec-cli-mcp-contract-check`。放弃原因是本次检查目标是项目文档与 TODO 状态，不是 CLI/MCP fixture 对齐。

2. **通过 `supportedWorkflowRecipeIds()` 注入 canonical ids。**
   - 原因：workflow recipe 的真实来源已经集中在 `dataspec-workflows.mjs`，直接复用可避免手写清单继续漂移。
   - 备选方案：从 CLI `workflow list` 输出解析。放弃原因是测试会多一层 CLI 参数和 IO 噪声，不如直接验证共享模块。

3. **对 `docs/ai-contracts.md` 和 `TODO.md` 分别报错。**
   - 原因：两个文件服务不同读者；前者是 AI 稳定契约，后者是项目状态/验收摘要。分开 issue code 能给出更精准修复建议。
   - 备选方案：只做全文任意位置包含检查。放弃原因是无法告诉维护者到底是哪份权威说明滞后。

## Risks / Trade-offs

- [Risk] 文档中因示例或历史说明出现 recipe id，导致误判为已同步。→ Mitigation：只解析 `.dataspec/workflows.md`、任务卡已完成能力等限定行；找不到限定行时按目标清单缺失报错，不用全文历史 mention 兜底。
- [Risk] 后续 TODO 表述调整导致行定位失效。→ Mitigation：检查函数接受文本输入并有 fixture 测试，表述调整时先补测试再改解析规则。
- [Risk] active changes 长期未归档导致 status-check 常态 warn。→ Mitigation：本次只新增 error 级 drift 检查，不改变既有 active change warning 行为。

## Migration Plan

1. 先补失败测试，覆盖 `docs/ai-contracts.md` 缺失 recipe id 和 `TODO.md` 缺失 recipe id 两类漂移。
2. 实现 status-check 输入读取与检查逻辑。
3. 更新 README 验证工具说明。
4. 运行 Node 测试、CLI 状态检查、OpenSpec strict 和 Git diff 检查。
5. 通过独立只读子 agent 评审后再 commit。

## Open Questions

无。当前 recipe id 来源和目标文档均已存在。
