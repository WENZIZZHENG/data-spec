# SDD 项目级完整工作约定

本文件是 data-spec 进入 SDD 后的完整执行规范，不依赖个人全局配置。是否进入 SDD 先按 `AGENTS.md` 的两段式流程判断；进入后再按本文选择 lite / standard / full。

## 1. 执行原则

- SDD 任务采用 OpenSpec-first：先形成 change artifacts，再实施代码。
- 默认在当前工作目录和当前分支中修改；不要为 SDD 流程创建 git worktree、切换分支或切换到隔离工作树，除非用户明确要求。
- 先定位影响范围、相关代码、测试、项目文档和现有 OpenSpec；避免无目的全仓库扫描。
- 发现已有 open change 明显覆盖当前需求时，更新该 change；否则创建新的 `openspec/changes/<change-id>/`。
- `change-id` 使用短横线命名，并表达用户可理解的意图。
- 实施中发现需求、设计、任务或验收标准变化时，先同步 OpenSpec artifacts，再继续改代码。

快速修 / hotfix 规则：

- 用户要求快速修时，若命中 `AGENTS.md` 的 SDD 硬触发，只能降为 SDD lite / hotfix，不能完全跳过 SDD。
- 若用户明确要求跳过 SDD，最终回复必须记录被跳过的风险、已做验证和建议补齐项。

## 2. SDD 分级

### lite

适用：

- 低风险、小范围、可回退的 SDD 任务。
- 不改变长期外部契约的轻量 CLI / MCP 行为调整。
- 单点非核心能力、小型用户可见修正、hotfix。

要求：

- 编码前至少完成简化 `proposal.md` 和 `tasks.md`。
- 若不写 spec delta，必须说明没有改变可观察契约或变更已由现有 spec 覆盖。
- 跑相关模块验证和 `openspec validate <change-id> --strict`。

### standard

适用：

- 默认 SDD 等级。
- 新增或调整用户可见行为、公共接口、跨模块协作、导入导出主流程、AI 可用工作流。
- 需要明确验收标准，但不涉及高风险迁移或安全边界。

要求：

- 编码前完成 `proposal.md`、`tasks.md` 和必要的 `specs/` delta。
- 有明显取舍、兼容性问题或迁移风险时补充简短 `design.md`。
- 跑相关模块验证、OpenSpec strict 校验，并记录验证结果。

### full

适用：

- 数据库迁移、存储结构、安全、权限、敏感凭据、核心数据一致性、架构或部署级变更。
- 会影响多个模块、历史数据或 AI 外部调用稳定性的变更。
- 关键生产 bug、多阶段交付或回滚成本较高的任务。

要求：

- 编码前完成 `proposal.md`、`tasks.md`、必要的 `specs/` delta 和 `design.md`。
- `design.md` 覆盖方案取舍、备选方案、风险、迁移 / 回滚、兼容性和验证策略。
- 完成全量或接近全量验证，并在 archive 前记录 `Verification Evidence`。

## 3. OpenSpec 使用规则

- OpenSpec 用于守住长期契约，不用于替代所有 TODO 或小修记录。
- 只有改变可观察行为、公共接口、数据模型、权限、安全、任务调度、持久化或跨模块能力时，才必须写 spec delta。
- 纯内部重构或测试补丁不改变可观察行为时，可不写 spec delta，但要在 `proposal.md` 或 `tasks.md` 中说明。
- 首次使用 OpenSpec、CLI 版本变化、命令失败、项目缺少 OpenSpec 结构或命令不确定时，才探测 CLI 版本、帮助和子命令；日常迭代沿用项目已有命令和 artifacts 格式。
- Artifact 写作规则、delta spec 格式和验证命令以项目已有 OpenSpec 结构、当前 CLI 输出和校验结果为准。
- CLI 不可用或缺少相关指引时，沿用项目已有格式；如果项目没有格式，至少手写 `proposal.md`、`tasks.md` 和必要 spec delta，并说明降级原因。

## 4. Artifacts 自检清单

编码前和最终验证前，按当前 SDD 分级检查 artifacts；发现偏差先修 artifacts，再继续实现或提交。

- `proposal.md`：说明问题、目标、非目标范围、用户可见影响和选择该分级的原因；没有 `TODO`、`TBD`、占位段落或矛盾表述。
- `tasks.md`：拆成可执行、可勾选、可验证的步骤；包含代码、测试、文档和 OpenSpec 验证；能对应验收标准。
- `specs/` delta：覆盖新增、修改或删除的能力；包含清晰 requirement 和 scenario；不把实现细节写成用户契约。
- `design.md`：full 或存在重要取舍时记录备选方案、选型理由、风险、迁移 / 回滚、兼容性和验证策略。
- 一致性：proposal、tasks、spec delta、design 与实际实现互相匹配。

## 5. 实现与文档

- 实现时遵循 `AGENTS.md` 的代码注释、最小改动、现有模式和验证要求。
- `proposal.md` 或 `design.md` 应说明本次变更需要遵循的既有模式、边界约束、兼容要求、风险点和关键取舍。
- SDD 任务的 artifacts 必须把实现质量当作验收条件，而不是只描述功能完成。
- 只同步本次变更直接影响且已存在的权威文档：
  - 用户可见能力、命令、接口、边界变化：`README.md`。
  - 待办、优先级或状态变化：`TODO.md`。
  - 长期取舍或方案原因：`DECISIONS.md` 或项目已有等价文档。
  - schema 或数据库变更：迁移脚本、字段说明、中文注释或项目约定的元数据。
- 不存在这些文件时不强行创建，除非本次变更确实需要长期记录或用户明确要求。

## 6. 验证要求

- SDD 任务必须验证 OpenSpec artifacts：优先运行 `openspec validate <change-id> --strict`。
- 同时运行与变更最相关的项目验证；没有统一命令时，选择最小有效验证集。
- OpenSpec CLI 或项目验证无法运行时，人工检查 artifacts 是否完整、一致、无占位，并说明原因、替代检查和建议后续命令。

data-spec 验证矩阵：

- 后端：`mvn test`。
- 前端：在 `dataspec-web` 运行 `pnpm test`、`pnpm build`。
- CLI / MCP / tools：`node --test tools/*.test.mjs`。
- OpenSpec：`openspec validate <change-id> --strict`；full 收口时运行 `openspec validate --all`。
- 通用检查：`git diff --check`。

## 7. 评审与归档

评审门禁按 `AGENTS.md` 第 7 节执行：

- SDD lite / standard：agent 优先；工具不可用时可结构化自审并记录原因。
- SDD full，或涉及安全、存储、OpenAPI 契约、CLI / MCP / AI 外部协议：必须独立 agent 评审。

归档规则：

- 实现和验证完成后，默认保留 open change，不自动 archive。
- 只有用户明确要求、项目约定要求，或变更已确认完成并需要合入基线 specs 时，才运行 `openspec archive <change-id>`。
- archive 前必须补充 `Verification` 或 `Verification Evidence` 小节；仅勾选任务清单不算验证证据。

## 8. 提交与输出

- 通过必要验证，并满足对应 OpenSpec 与评审门禁后，默认创建本地 commit。
- commit 只 stage 本次任务产生的变更；仓库状态安全定义见 `AGENTS.md`。
- commit message 使用中文，概括用户可理解的变更结果。
- 不主动 push。
- 最终回复只说明适用项：改了什么、OpenSpec change 路径和状态、如何验证、是否 commit / archive、剩余风险或下一步；不适用项可省略。
