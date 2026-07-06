## Why

字段重命名、废弃或合并标准前，DataSpec 目前只能说明模板、导入来源、历史 SQL 检查和快照等项目内影响，无法告诉用户业务仓库里哪些 SQL、迁移文件、ORM 模型或配置仍在引用该字段。P6-88 需要补上只读业务代码字段引用索引，让用户和 AI 在改标准前能看到文件位置、引用类型、置信度和重命名风险。

## What Changes

- 新增业务代码字段引用索引能力：按 `.dataspec/config.json` 的 `defaultPaths` 或显式路径扫描业务仓库文件，输出字段引用位置、引用类型、置信度、建议动作和重命名风险。
- 新增 `index-refs` CLI 命令：面向 AI/本地用户输出稳定 JSON；默认只读、不调用外部网络、不自动修改业务代码。
- 扩展字段影响分析：字段影响报告可包含业务代码引用摘要和明细，供前端字段影响弹窗展示。
- 更新 CLI/MCP 契约 fixture、README/TODO 和 OpenSpec 验证证据，确保 AI 能稳定识别该能力边界。
- 不做完整 AST 平台、不解析所有语言、不自动改业务代码；第一版优先覆盖 SQL、DDL、迁移文件、常见 schema/model/config 文本引用。

## Capabilities

### New Capabilities
- `code-field-reference-index`: 覆盖业务仓库字段引用扫描、引用结果结构、只读安全边界、重命名风险与 defaultPaths 约束。

### Modified Capabilities
- `dataspec-cli`: 新增 `index-refs` 命令、稳定 JSON 输出、退出码、安全元数据和契约 fixture 覆盖。
- `field-impact-analysis`: 字段影响报告新增业务代码引用摘要与前端可展示影响项。

## Impact

- 后端：可能新增只读引用索引 API、DTO/model/service，并扩展字段影响报告模型。
- CLI/tools：新增 `index-refs` 命令、扫描实现、契约 fixture 和 Node 测试；复用 `.dataspec/config.json`、defaultPaths 和现有错误诊断风格。
- 前端：字段库影响弹窗展示业务代码引用摘要和风险项。
- 文档/OpenSpec：新增 change artifacts，更新 README/TODO 和验证证据。
- 安全边界：不读取业务数据行，不保存凭据，不扫描 defaultPaths 外的大目录，输出路径和引用片段需要避免泄漏 token、password、Authorization、JDBC URL、DSN 或连接串。
