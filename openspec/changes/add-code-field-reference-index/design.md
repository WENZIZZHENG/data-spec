## Context

DataSpec 已有字段影响分析，但它聚合的是项目内对象：表模板、反向导入来源、近期 SQL 检查记录、标准快照和代码集。业务仓库内的 SQL、迁移文件、ORM 模型和配置文件通常只存在于用户本地仓库，DataSpec 后端不能安全假设自己能访问这些路径。

P6-88 的第一版要补“改字段前先看业务仓库引用”的闭环，同时保持个人/小团队工具的低成本：默认从 `.dataspec/config.json` 的 `defaultPaths` 或显式路径扫描，只读输出 JSON，供 AI 判断是否继续重命名、废弃或等待人工确认。

## Goals / Non-Goals

**Goals:**
- 提供 `index-refs` CLI，只读扫描业务仓库内字段引用。
- 输出稳定 JSON：字段名、别名、引用类型、文件、行列、置信度、重命名风险、建议动作和汇总。
- 遵守 `defaultPaths`，未传路径且未配置 defaultPaths 时停止，避免全仓库误扫。
- 扩展字段影响报告和前端展示所需的 `CODE_REFERENCE` 类型，使引用摘要能作为字段影响来源出现。
- 对输出片段做敏感信息脱敏，不输出 token、password、Authorization、完整 JDBC URL、DSN 或连接串。

**Non-Goals:**
- 不解析所有语言 AST，不建设完整代码智能平台。
- 不自动修改业务代码，不生成批量重命名 patch。
- 不新增数据库持久化表来保存业务代码引用明细。
- 不让后端根据用户传入路径任意读取服务器文件系统。

## Decisions

1. **以 CLI 本地扫描作为第一版入口。**
   - 选择：新增 `dataspec index-refs`，运行位置是业务仓库本地工作区，复用现有 `.dataspec/config.json`、`defaultPaths`、`SKIPPED_SCAN_DIRECTORIES` 和 CLI 错误风格。
   - 备选：新增后端 API 直接读取路径。放弃原因是后端通常不在业务仓库内，且任意路径扫描容易越权读取服务器文件。

2. **使用轻量确定性文本扫描，按引用类型给置信度。**
   - 选择：优先覆盖 SQL/DDL/迁移文件、MyBatis/XML、常见 Java/TS/JS/Python/Go model/schema/config 文件。SQL/DDL 标识符、点号路径、quoted/backtick identifier 记为较高置信；普通单词命中按文件类型降级。
   - 备选：引入 AST parser 或 language server。放弃原因是依赖和维护成本高，第一版只需要风险预警而不是精确重构。

3. **把重命名风险作为摘要，不把风险判断伪装成自动迁移建议。**
   - 选择：根据命中数量、最高置信度、referenceKind 和文件类型生成 `renameRisk`，并输出 `suggestedAction`/`nextActions`。
   - 备选：直接生成代码修改建议。放弃原因是跨语言自动改名风险高，且 P6-88 明确不自动改业务代码。

4. **字段影响集成只扩展可读摘要类型。**
   - 选择：新增 `CODE_REFERENCE` impact type 和 `codeReferenceImpactCount` 汇总字段；前端字段影响弹窗能展示该类型。第一版不持久化引用明细，CLI 输出仍是业务仓库引用的权威证据。
   - 备选：新增后端持久化索引表并由 CLI 上传。放弃原因是会引入存储、脱敏、过期和权限复杂度，超出第一版最小闭环。

## Risks / Trade-offs

- **误报或漏报** → 输出 `confidence` 和 `possibleReference`，高风险操作仍提示人工确认。
- **扫描过大目录导致慢或泄漏无关文件路径** → 默认只扫 explicit paths 或 defaultPaths，并跳过 `.git`、`node_modules`、`dist`、`target` 等目录。
- **输出片段包含敏感信息** → 所有 `snippet`、诊断和错误文本统一脱敏；测试覆盖 password/token/JDBC/DSN 场景。
- **前端无法直接读取本地业务仓库** → 第一版前端只展示后端报告里的摘要类型，不承诺浏览器直接扫描本地文件；业务仓库证据由 CLI JSON 提供。
- **字段别名不完整导致召回不足** → CLI 支持多次 `--field`/`--alias`，后续可再接入服务端字段详情自动补 aliases。

## Migration Plan

- 新增能力为向后兼容：不改变现有字段影响 API 的必填字段，不改变既有 CLI 命令语义。
- CLI 新命令失败只返回 `2` 和非敏感诊断，不影响 lint、changed、doctor 等已有命令。
- 若后续需要持久化索引或前端上传索引 JSON，单独开 SDD full 处理存储和敏感信息边界。
