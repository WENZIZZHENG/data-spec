## Context

当前 DataSpec 已有三条 AI 相关核心链路：

- `AiContextExportService` 生成建表 Prompt 和 SQL 修正 Prompt，但只返回文本，不留任务级记录。
- `SqlLintService` 会保存 SQL 检查记录，包含 originalSql、fixedSql、issues 和标准快照引用，但它不是面向 AI 作业回放的统一入口。
- `DdlGeneratorService` 会返回 DDL、lintResult 和当前标准快照，但结果不持久化，后续无法回放。

P6-3 的设计目标是追加一个轻量 AI 作业记录层，把这些已有输出串起来。它不负责调用外部大模型，也不保存第三方 API key；它只记录 DataSpec 本地生成/检查/修复时已经产生的输入、输出和上下文。

## Goals / Non-Goals

**Goals:**

- 新增 AI 作业记录表和 API，按项目分页查看、查看详情、复制回放 JSON 和 CLI 命令。
- 记录作业类型、输入摘要、输入 payload、输出 payload、标准快照、prompt 模板版本、关联 SQL 检查记录和状态。
- 接入建表 Prompt、SQL 修正 Prompt、SQL lint/fixedSql 和 DDL preview 四类已有链路。
- 前端提供轻量“AI 回放”页面，方便用户和 AI 回看某次生成/修复的上下文。
- 保持现有 SQL 检查记录、DDL 预览和 AI Prompt API 的响应兼容；新增记录不阻断原有主流程。

**Non-Goals:**

- 不内置外部 LLM 调用，不保存 OpenAI/第三方 API key。
- 不做长文本聊天会话管理，不做多人协同审批。
- 不替代 `ds_sql_check_record`；SQL 检查记录仍保存 lint/fixedSql 的事实数据。
- 不实现历史标准快照完整回放导出；P6-21 会处理按快照导出与历史差异。

## Decisions

1. 新增独立 `ds_ai_job_record` 表，而不是继续扩展 `ds_sql_check_record`。
   - 原因：P6-3 要覆盖 prompt、DDL、lint/fixedSql 多种作业，SQL 检查只是其中一种；独立表能表达统一作业语义。
   - 替代方案：给 `ds_sql_check_record` 增加 prompt 字段。暂不采用，会让 SQL 检查记录承担太多非 lint 职责。

2. 作业记录保存 JSON 文本，不在第一版拆成多张子表。
   - 原因：输入/输出结构随作业类型变化较大，个人版优先可回放和可复制；后续统计视图可在 P6-27 或 P6-32 中再建聚合模型。
   - 约束：API 详情会反序列化为通用对象，反序列化失败时仍返回原始 JSON 字符串。

3. 使用固定 prompt 模板版本常量。
   - 原因：P6-31 会做完整 Prompt 模板版本化和评测；P6-3 只需要能让记录说明“当时是哪一版内置模板”。
   - 取舍：第一版版本号可从代码常量开始，例如 `create-table-prompt@1`、`fix-sql-prompt@1`。

4. 记录失败不应破坏主流程。
   - 原因：AI 回放是辅助复盘能力，不能让 SQL lint、Prompt 生成或 DDL 预览因为记录写入异常而失败。
   - 约束：集成点捕获记录写入异常并记录 warn；直接调用 AI 作业 API 仍返回明确业务错误。

## Risks / Trade-offs

- [记录体积增长] → 第一版只分页列表，详情按需读取；大字段库性能与归档后续放入 P6-16/P6-24。
- [JSON 契约漂移] → 输出 `jobType`、`promptVersion`、`standardSnapshot*` 等稳定字段；输入/输出 payload 作为作业内结构，后续由 P6-12 加 golden。
- [敏感数据写入] → 不记录 token/password/API key；数据库直连类 payload 第一版不接入；SQL 文本和业务描述由用户主动提交，按现有记录边界保存。
- [重复记录] → Prompt/fix/lint/DDL 四个入口各自记录一次；后续如需串联成 batch run 放入 P6-26。

## Migration Plan

1. 新增 V10 Flyway 迁移创建 `ds_ai_job_record` 表和项目/类型/时间索引。
2. 新增后端 entity/mapper/repository/service/controller 和单元测试。
3. 在 AI Prompt、SQL lint 和 DDL preview 服务中注入记录服务并以 best-effort 方式写入。
4. 新增前端 API/types/页面和展示工具测试。
5. 更新 README/TODO/OpenSpec tasks，运行完整验证后提交。
6. 回滚时删除新增模块和入口；数据表可保留为空闲历史表，不影响现有链路。

## Open Questions

- CLI/MCP 是否要在第一版也新增 `ai-jobs` 查询命令？本轮优先 API/前端；CLI/MCP 可在 P6-11 工作流模板中补齐。
- DDL preview 是否要单独保存完整 lint issues？第一版输出 payload 包含 `lintResult`，无需再拆字段。
