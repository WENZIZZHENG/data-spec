## Why

AI、CLI、MCP 和业务仓库会长期引用字段、枚举、规则和快照。若只依赖可变的字段名、显示名或自然语言说明，字段重命名、合并、废弃或标准升级后，AI 输出很容易继续引用不存在或过期的标准对象，形成看似合规但实际漂移的结果。

本变更接在 AI Context 安全边界之后，先建立稳定引用和确定性后置校验，让后续查询 DSL、消费端兼容和测试数据包可以依赖同一套标准对象引用语义。

## What Changes

- 新增标准对象稳定引用契约：为字段和枚举优先输出 `stableRef`、`canonicalRef`、历史别名和解析状态；规则和快照提供只读稳定引用摘要。
- 新增引用解析能力：支持把字段名、别名、历史名、stableRef 或 enum/rule ref 解析成当前标准对象，并返回是否命中当前对象、废弃对象、合并对象或未知引用。
- 新增 AI 输出后置校验能力：对 AI 生成的 SQL、DDL、Markdown 或 JSON 文本做本地确定性检查，识别不存在字段、过期字段、错误枚举值、未知规则码、旧快照引用和缺少证据的标准声明。
- CLI/MCP/API 暴露稳定 JSON：提供可被 AI agent 调用的 resolve/check 入口，输出 `PASS`/`WARN`/`FAIL`、问题列表、建议替代引用和 evidence links。
- AI Context、证据包和 schema registry 增加 additive metadata，帮助消费端在不破坏旧字段的前提下读取 stable refs 和 post-check 结果。
- 第一版不迁移历史业务仓库、不自动改写 AI 输出、不强制用户手填 stableRef；优先覆盖字段、枚举、规则和标准快照引用。

## Capabilities

### New Capabilities

- `stable-standard-references`: 标准对象 stableRef/canonicalRef、历史别名和引用解析结果的公共契约。
- `ai-output-postcheck`: AI 产物后置校验、幻觉引用拦截、替代建议和证据链接的公共契约。

### Modified Capabilities

- `field-model`: 字段 metadata 和 AI export 需要 additive 输出 stableRef、canonicalRef、aliasHistory 和 replacement ref 摘要。
- `field-lifecycle`: 废弃/停用/替代字段需要参与引用解析和 post-check stale ref 判断。
- `field-standard-search`: 搜索结果需要返回 stableRef/canonicalRef，并能按稳定引用解释旧字段名命中。
- `ai-context-package`: field catalog、manifest 和 guidance 需要暴露稳定引用、别名历史和 post-check 使用说明。
- `standard-schema-registry`: contract detail 需要登记 stable ref 字段、引用解析结果和 post-check result schema。
- `ai-evidence-package`: evidence package 需要可携带 post-check 摘要、失败引用和建议修复命令。
- `dataspec-cli`: CLI 需要新增或扩展 stable-ref resolve 与 ai-output check 命令的稳定 JSON 契约。
- `dataspec-mcp`: MCP 需要暴露对应 tool/resource，供 AI agent 在采纳输出前调用。
- `cli-mcp-contract-fixtures`: 新增 resolve/check fixture，防止 CLI/MCP 外部协议漂移。

## Impact

- 后端：新增标准引用解析 service/API、AI 输出后置校验 service/API；扩展字段导出、枚举/规则/快照引用摘要、schema registry 和 evidence package。
- CLI/MCP：新增或扩展 resolve/check 命令和 tool，保持 JSON 输出稳定、错误诊断可读且 secret-safe。
- 前端：第一版可先提供最小校验摘要入口或复用现有 AI 回放/导出页面展示结果；完整工作台体验可后续增强。
- OpenSpec：新增两个能力 spec，并对字段模型、生命周期、搜索、AI Context、Schema Registry、Evidence、CLI/MCP 和 fixture 契约做 delta。
- 风险：涉及 API/CLI/MCP/AI 外部协议，按 SDD standard 偏 full 执行；实现与 commit 前必须进行独立子 agent 评审。
