# SDD 项目级完整工作约定

本文件是 data-spec 进入 SDD 后的完整执行规范，不依赖个人全局配置。是否进入 SDD 先按 `AGENTS.md` 判断；进入后再按本文选择 SDD lite / SDD standard / SDD full。

## 1. 执行原则

- SDD 任务采用 OpenSpec-first：先形成 change artifacts，再实施代码。
- 默认在当前工作目录和当前分支中修改；不要为 SDD 流程创建 git worktree、切换分支或切换到隔离工作树，除非用户明确要求。
- 先定位影响范围、相关代码、测试、项目文档和现有 OpenSpec；避免无目的全仓库扫描。
- 发现已有 open change 明显覆盖当前需求时，更新该 change；否则创建新的 `openspec/changes/<change-id>/`。
- `change-id` 使用短横线命名，并表达用户可理解的意图。
- 实施中发现需求、设计、任务或验收标准变化时，先同步 OpenSpec artifacts，再继续改代码。

hotfix 规则：

- 快速修命中 SDD 硬触发时，默认降为 SDD lite / hotfix。
- 可跳过的是完整 SDD 文档流程，不可跳过最小安全门禁：hotfix 记录、相关验证、`AGENTS.md` 第 7 节要求的评审。
- 安全、凭据、存储、权限、OpenAPI 契约、CLI / MCP / AI 外部协议类变更如果用户坚持完全跳过最小门禁，必须停止在实施或交付前，并说明阻塞原因。

## 2. SDD 等级速查

| 触发类型 | 默认等级 | 说明 |
| --- | --- | --- |
| 低风险、可回退、hotfix、不改长期公共契约 | SDD lite | 可用最小记录 |
| 用户可见契约、跨模块协作、导入导出主流程、AI 工作流 | SDD standard | 需要明确验收和 spec delta |
| OpenAPI / CLI / MCP / AI 外部协议 | SDD standard；breaking、高兼容风险或多模块时 SDD full | 始终执行 `AGENTS.md` 第 7 节强制独立评审门禁 |
| 安全、凭据、权限、存储、数据一致性、迁移 | SDD full | 必须独立 agent 评审 |
| 构建、部署、架构 | SDD full | 回滚成本高 |
| 任务调度、核心解析 / 校验链路 | SDD standard；影响历史数据或多模块时 full | 按影响范围升级 |

## 3. SDD 分级要求

### SDD lite

适用：

- 低风险、小范围、可回退的 SDD 任务。
- 不改变长期公共契约的轻量 CLI / MCP 行为调整。
- 单点非核心能力、小型用户可见修正、hotfix。

要求：

- 可使用单文件最小记录；允许先修复，commit 前补齐记录。
- 最小记录必须包含：背景 / 原因、变更内容、验证结果。
- 若不写 spec delta，必须说明没有改变可观察契约或变更已由现有 spec 覆盖。
- 运行相关模块验证和 `openspec validate <change-id> --strict`。

### SDD standard

适用：

- 新增或调整用户可见行为、公共接口、跨模块协作、导入导出主流程、AI 可用工作流。
- 需要明确验收标准，但不涉及高风险迁移或安全边界。

要求：

- 编码前完成 `proposal.md`、`tasks.md` 和必要的 `specs/` delta。
- 有明显取舍、兼容性问题或迁移风险时补充简短 `design.md`。
- 运行相关模块验证和 `openspec validate <change-id> --strict`，并记录验证结果。

### SDD full

适用：

- 数据库迁移、存储结构、安全、权限、敏感凭据、核心数据一致性、架构或部署级变更。
- 会影响多个模块、历史数据或 AI 外部调用稳定性的变更。
- 关键生产 bug、多阶段交付或回滚成本较高的任务。

要求：

- 编码前完成 `proposal.md`、`tasks.md`、必要的 `specs/` delta 和 `design.md`。
- `design.md` 覆盖方案取舍、备选方案、风险、迁移 / 回滚、兼容性和验证策略。
- 完成接近全量验证，并在 commit、archive 或最终交付前记录 `Verification Evidence`。
- 必须满足 `AGENTS.md` 的强制 agent 评审门禁。

## 4. OpenSpec 使用规则

- OpenSpec 用于守住长期契约，不用于替代所有 TODO 或小修记录。
- 只有改变可观察行为、公共接口、数据模型、权限、安全、任务调度、持久化或跨模块能力时，才必须写 spec delta。
- 纯内部重构或测试补丁不改变可观察行为时，可不写 spec delta，但要在 `proposal.md` 或 `tasks.md` 中说明。
- 首次使用 OpenSpec、CLI 版本变化、命令失败、项目缺少 OpenSpec 结构或命令不确定时，才探测 CLI 版本、帮助和子命令；日常迭代沿用项目已有命令和 artifacts 格式。
- Artifact 写作规则、delta spec 格式和验证命令以项目已有 OpenSpec 结构、当前 CLI 输出和校验结果为准。
- CLI 不可用或缺少相关指引时，沿用项目已有格式；如果项目没有格式，至少手写必要 artifacts，并说明降级原因。

## 5. 最小 artifact 模板

### proposal.md

```markdown
## 背景
- 为什么要改：

## 目标
- 本次交付：

## 非目标
- 本次不做：

## 验收
- 可验证结果：
```

### tasks.md

```markdown
## 任务
- [ ] 更新 artifacts
- [ ] 实现最小改动
- [ ] 补充必要注释和字段说明
- [ ] 补充或更新测试
- [ ] 运行验证
- [ ] 记录 Verification Evidence
```

### spec delta

```markdown
## ADDED Requirements
### Requirement: 中文能力名
系统 SHALL ...

#### Scenario: 中文场景名
- WHEN ...
- THEN ...
```

### Verification Evidence

```markdown
## Verification Evidence
- 命令：
- 结果：
- 评审：
- 未覆盖风险：
```

SDD lite / hotfix 可只保留“背景 / 变更 / 验证”三段最小记录；SDD full 不得省略 `design.md`。所有 SDD 等级在 commit、archive 或最终交付前都必须记录新鲜 `Verification Evidence`。

## 6. Artifacts 自检清单

- `proposal.md`：说明问题、目标、非目标、用户可见影响和选择该分级的原因；没有 `TODO`、`TBD`、占位段落或矛盾表述。
- `tasks.md`：拆成可执行、可勾选、可验证的步骤；包含代码、测试、文档和 OpenSpec 验证；能对应验收标准。
- `specs/` delta：覆盖新增、修改或删除的能力；包含清晰 requirement 和 scenario；不把实现细节写成用户契约。
- `design.md`：full 或存在重要取舍时记录备选方案、选型理由、风险、迁移 / 回滚、兼容性和验证策略。
- 一致性：proposal、tasks、spec delta、design 与实际实现互相匹配。

## 7. 实现、文档与验证

- 实现时遵循 `AGENTS.md` 的代码注释、最小改动、现有模式、验证、评审、Git 和安全规则。
- SDD 过程需要工具能力时，只使用当前会话已提供且可调用的工具能力，不依赖历史或外部已移除能力。
- `proposal.md` 或 `design.md` 应说明本次变更需要遵循的既有模式、边界约束、兼容要求、风险点和关键取舍。
- SDD 任务的 artifacts 必须把实现质量当作验收条件，而不是只描述功能完成。
- 涉及新增或调整公共代码表面、API、DTO、配置、schema、数据库对象、CLI / MCP / AI 协议时，验收必须包含必要注释、字段说明或 schema description；公共表面缺少语义说明时不得视为实现完成。
- 项目文档同步按 `AGENTS.md` 第 8 节执行；OpenSpec artifacts 是 SDD 主记录，非直接影响不更新项目文档。
- SDD 任务必须验证 OpenSpec artifacts：优先运行 `openspec validate <change-id> --strict`。
- 同时按 `AGENTS.md` 的候选验证命令选择最小有效项目验证集。
- OpenSpec CLI 或项目验证无法运行时，人工检查 artifacts 是否完整、一致、无占位，并说明原因、替代检查和建议后续命令。

## 8. 评审、归档与提交

- 评审门禁按 `AGENTS.md` 第 7 节执行。
- 修改 `AGENTS.md` 或 `SDD.md` 属于项目规范自修改，最终交付、commit 或 archive 前必须按 `AGENTS.md` 第 7 节由主 agent 启动一次独立子 agent 只读复评，并在完成后使用当前会话可用的子 agent 关闭工具关闭该子 agent；复评子 agent 不递归触发同类复评。
- 实现和验证完成后，默认保留 open change，不自动 archive。
- 只有用户明确要求、项目约定要求，或变更已确认完成并需要合入基线 specs 时，才运行 `openspec archive <change-id>`。
- archive 顺序：补本次交付的 `Verification Evidence` -> 运行 change strict 校验 -> 满足评审门禁 -> 执行 archive -> 运行 `openspec validate --all` 或相关基线校验 -> 追加 archive 后验证结果 -> 按 `AGENTS.md` Git 规则处理 archive 变更。
- archive 前必须补充 `Verification` 或 `Verification Evidence` 小节；仅勾选任务清单不算验证证据。
- commit / push 规则按 `AGENTS.md` 第 9 节执行。
- 最终回复只说明适用项：改了什么、OpenSpec change 路径和状态、如何验证、是否 commit / archive、剩余风险或下一步；不适用项可省略。
