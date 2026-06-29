## Context

该变更来自 TODO P6-49「自然语言需求到标准候选草案」。DataSpec 已有字段推荐/检索、业务术语表、标准候选 Inbox、表模板和生成器，但用户或 AI 从一句自然语言需求开始时，还缺少一个把这些能力编排成草案的入口。

## Goals / Non-Goals

**Goals:**
- 新增需求草案 API 和前端入口，输入业务描述、目标表名和可选分组，输出 matchedFields、missingCandidates、ambiguousTerms、recommendedTemplate、nextActions 和可复制 Prompt。
- 输入一段建表需求后，系统能列出建议采用的标准字段、需要新增的候选字段和不确定问题；结果可继续进入 DDL 预览或标准候选 Inbox。

**Non-Goals:**
- 第一版不调用外部 LLM，不自动写入字段库，不承诺完整领域建模，只做确定性检索和模板化草案。
- 第一版不新增持久化表，不自动创建标准候选，不新增 CLI/MCP 命令。

## Decisions

1. **做只读编排层，不新增落库模型。**
   - 原因：草案是一次性分析结果，真正写入字段库或候选 Inbox 仍应通过现有确认流程完成。

2. **复用字段推荐、字段检索、业务术语表和模板列表。**
   - 原因：P6-49 的价值在于把已有能力串起来，避免重新实现语义匹配。

3. **第一版先提供 API + 前端入口。**
   - 原因：用户需要在页面里快速试用；CLI/MCP 后续可复用同一 API 契约补齐。

## Risks / Trade-offs

- [Risk] 确定性匹配可能漏掉业务隐含字段。→ Mitigation：输出 missingCandidates 和 ambiguousTerms，明确需要人工/AI 再确认的问题。
- [Risk] 草案结果被误认为已写入标准。→ Mitigation：响应和前端文案明确“草案不落库”，进入 Inbox 需后续显式创建。
- [Risk] 模板推荐不准。→ Mitigation：第一版按目标表名、描述和模板字段命中分数排序，并在结果里展示命中原因。

## Data Shape

- Request: `projectId`, `description`, `targetTableName`, `groupHint`, `limit`
- Response:
  - `matchedFields`: 标准字段、分数、命中原因、是否建议采用
  - `missingCandidates`: 候选字段名、显示名、类型、证据、置信度、建议进入 Inbox 的 payload
  - `ambiguousTerms`: 词项、原因、可选字段
  - `recommendedTemplate`: 模板 id/name/score/matchReasons
  - `nextActions`: 面向人和 AI 的下一步
  - `copyablePrompt`: 可复制给 AI 的草案 prompt
