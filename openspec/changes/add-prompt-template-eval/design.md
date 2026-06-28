## Context

DataSpec 目前已经在建表 Prompt、SQL 修正 Prompt、SQL lint/fixedSql 和 DDL preview 中写入 `promptVersion`，但版本值分散在各服务的字符串常量里。Prompt 文本也缺少 fixture/golden 评测，后续调整输出要求时，`mvn test` 很难发现 Markdown 段落、JSON 片段或 SQL 输出约束被破坏。

## Goals / Non-Goals

**Goals:**
- 用一个后端 registry 统一声明 Prompt/AI 生成任务模板的 key、版本、场景和输出约束。
- 让现有 AI job 记录的 `promptVersion` 来自 registry，保持回放可追溯。
- 新增可本地执行的模板评测，检查必备段落/输出约束，并在 golden 文本变化时输出 diff。
- 把评测接入 `mvn test`，不依赖外部 LLM、API key 或联网服务。

**Non-Goals:**
- 不引入在线 prompt 实验平台。
- 不调用外部模型评判输出质量。
- 不改现有 Prompt API 的主响应语义，仍返回 prompt 文本。
- 不实现前端 Prompt 管理页面；第一版以后端 API/测试和文档为准。

## Decisions

1. 新增 `PromptTemplateRegistry`，而不是继续散落字符串常量。
   - 这样 DDL、lint、prompt 生成和后续推荐解释都能共享同一份版本定义。
   - 备选方案是只新增文档表，但无法让测试和 AI job 记录强制引用。

2. 评测使用确定性规则和 golden fixture。
   - 第一版只验证模板输出是否包含必备段落、输出格式约束、版本标记和标准引用，并对生成文本做 golden diff。
   - 备选方案是接入 `promptfoo` 或外部 LLM 评分；这会引入密钥、网络和非确定性，不适合作为默认验证入口。

3. Prompt API 保持返回 `String`，版本信息写入 prompt 文本和 AI job 记录。
   - 这样前端和现有调用方无需同步改类型。
   - 模板列表和评测结果通过新增 API 暴露给 AI 或后续前端使用。

4. `promptVersion` 使用 registry 的 `templateKey@version` 稳定格式。
   - 现有 `create-table-prompt@1`、`fix-sql-prompt@1`、`sql-lint-fix@1`、`ddl-preview@1` 继续兼容。
   - 未来升级模板时新增版本，不覆盖历史版本。

## Risks / Trade-offs

- [Risk] Golden 文本断言会让有意改 prompt 的 diff 更明显，也可能增加维护成本。→ 通过 fixtures 命名和 diff 输出让变更可审阅，避免静默漂移。
- [Risk] 只做确定性评测，不能证明外部模型效果更好。→ 第一版目标是防契约破坏，模型效果实验留给后续。
- [Risk] Registry 与 prompt 文本可能再次分叉。→ Prompt 生成时直接读取 registry 版本和必备约束，测试断言 AI job 记录版本必须来自 registry。
