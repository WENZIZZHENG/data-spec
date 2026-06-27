## Context

当前 SQL 能力由多个入口共同暴露：`SqlParserService` 负责 SQL DDL 解析，`SqlLintService` 负责 lint/fixedSql/检查记录，`DdlGeneratorService` 生成 PostgreSQL 风格 DDL，`ReverseImportService` 与 `DatabaseReverseImportService` 支持 SQL 文本和数据库直连反向导入。它们已经能处理 PostgreSQL/MySQL 的常见场景，但没有统一描述“方言是什么、能力是否完全支持、是否降级、下一步该怎么处理”。

P6-22 采用 `standard` 级别，因为它改变 API/CLI/前端可见结果，但不新增存储模型、不执行数据库写入，也不重写 parser。

## Goals / Non-Goals

**Goals:**

- 提供可复用的 PostgreSQL/MySQL 方言能力矩阵，覆盖 SQL 文本和数据库直连入口。
- 在 lint/fixedSql、DDL 生成、SQL 反向导入和数据库反向导入结果中返回统一 `dialectDiagnostics`。
- 让前端和 CLI 能展示当前方言、降级原因和 next action。
- 用 fixture/单测锁定 PostgreSQL/MySQL 的高价值能力边界。

**Non-Goals:**

- 不支持 Oracle/SQL Server 全量语法。
- 不手写完整 SQL parser，不替代 JSqlParser。
- 不执行 schema 迁移，不新增数据库写入行为。
- 不把未经测试的方言能力标记为 `SUPPORTED`。

## Decisions

1. **新增统一诊断模型而不是把字符串塞进各结果字段。**
   - 方案：新增 `DialectDiagnostic` 与 `DialectCapability`，在 `LintResult`、`DdlGenerateResult`、`ReverseImportPreview` 中复用。
   - 原因：AI/CLI 需要稳定 JSON 字段，前端也能按 `level/code/capability` 渲染，不依赖自然语言解析。
   - 替代方案：只在 README 写能力矩阵。该方案无法让运行时告诉 AI 当前请求是否降级。

2. **能力矩阵先做静态确定性服务。**
   - 方案：新增 `SqlDialectCompatibilityService`，按入口、SQL 文本特征和数据库类型输出诊断。
   - 原因：第一版不需要外部依赖或复杂 AST；静态矩阵能稳定测试和文档化。
   - 替代方案：接入 SQLFluff/其他 parser。该方案依赖和集成成本高，超出本轮“最小可交付”边界。

3. **SQL 文本方言识别采用保守启发式。**
   - 方案：识别 MySQL 特征如反引号、`AUTO_INCREMENT`、`ENGINE=`、`DEFAULT CHARSET`、inline `COMMENT`；否则默认 PostgreSQL，并在不确定时输出 `UNKNOWN_DIALECT` 提示。
   - 原因：现有 lint API 没有 dialect 参数，保守识别能兼容旧调用且给出机器可读提示。
   - 替代方案：强制新增必填 `dialect` 参数。会破坏现有 API/CLI/MCP 调用。

4. **DDL 生成明确声明目标为 PostgreSQL。**
   - 方案：生成结果直接返回 PostgreSQL 诊断，并提示 MySQL 等方言需二次转换。
   - 原因：现有生成器就是 PostgreSQL DDL，不能把它标成通用 SQL。

## Risks / Trade-offs

- [Risk] 启发式方言识别可能误判混合 SQL。→ Mitigation: 输出 `UNKNOWN_DIALECT` 或 `MIXED_DIALECT_HINT` 诊断，建议显式选择数据库直连或后续 dialect 参数。
- [Risk] 新增字段可能影响前端类型。→ Mitigation: 更新 `schema.ts` 手工类型和 `types/index.ts`，运行 `pnpm build`。
- [Risk] 矩阵过度承诺。→ Mitigation: 默认只把已有测试覆盖能力标记为 `SUPPORTED`，部分能力用 `PARTIAL` 或 `UNSUPPORTED`。
- [Risk] CLI 文本输出变吵。→ Mitigation: 文本模式只显示摘要和 WARNING/INFO；JSON 保留完整结构。

## Migration Plan

- API 新增可选字段，不删除旧字段，保持向后兼容。
- SQL 检查记录已有 `issues_json` 且本轮不新增列；新诊断通过实时 lint 响应和 AI 回放输出 JSON 保留。
- README 和 TODO 同步第一版能力与边界。
