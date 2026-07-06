## Context

DataSpec 已能导出完整 AI Context zip，也支持 scope/query/status/limit 和 AI profile 默认裁剪。但当前导出链路是“先选择参数再看结果”，AI 或用户无法在导出前判断 token 预算是否够、哪些资源会被舍弃、低预算会带来什么质量风险。

P6-90 的第一版需要补一个只读预算 planner。它不是精确 tokenizer，也不调用外部 LLM；它基于 DataSpec 已有字段目录、规则、使用示例、业务术语、能力清单和 profile/scope 参数做确定性估算，输出可机器读取的裁剪计划和风险说明。

## Goals / Non-Goals

**Goals:**
- 提供后端只读预算计划 API，输出稳定 JSON。
- 提供 CLI `context-budget plan`，方便 AI agent 在导出 Context 前做 preflight。
- 前端 AI Context 页面展示预算估算、推荐导出参数、质量风险和降级动作。
- 复用现有 scoped export/profile 语义；planner 推荐参数，不自动覆盖用户参数。
- 输出只包含计数、估算、资源名、风险和建议，不包含完整字段目录或敏感输入。

**Non-Goals:**
- 不实现模型专属精确 tokenizer。
- 不调用外部 LLM 或把标准内容上传到外部服务。
- 不自动生成或下载 AI Context zip；导出仍走现有 export-context/package 流程。
- 不做复杂语义检索或向量召回；第一版用现有 scope/query/limit 与轻量权重。

## Decisions

1. **新增 `ai-context-budget-planner` 只读能力，而不是改变现有导出接口默认行为。**
   - 选择：新增 `/api/ai-context/budget/plan` 和 CLI `context-budget plan`。
   - 原因：既有导出 API 已被 CLI/MCP/前端依赖，预算 planner 作为 preflight 更兼容。
   - 备选：直接让 package/download 返回预算摘要。放弃原因是 zip 下载响应不适合携带交互式风险和候选参数。

2. **使用确定性估算，不引入 tokenizer 依赖。**
   - 选择：按 JSON/YAML/Markdown 字符数和资源权重估算 token，使用保守比例和固定 overhead。
   - 原因：项目定位是个人/小团队本地工具，依赖外部 tokenizer 或模型配置会增加复杂度和不稳定性。
   - 备选：接入模型 tokenizer。放弃原因是模型差异大，且当前验收只需要预算等级和取舍说明。

3. **输出资源级计划，而不是直接输出裁剪后的内容。**
   - 选择：`selectedArtifacts[]` / `droppedArtifacts[]` 描述 artifact、estimatedTokens、reason、riskImpact、appliedScope。
   - 原因：AI 需要理解“为什么保留/丢弃”，而不是只拿到一个最终 zip。
   - 备选：返回完整裁剪 payload。放弃原因是会扩大响应、重复现有导出能力，也增加敏感信息泄露面。

4. **把质量风险分为 `LOW` / `MEDIUM` / `HIGH`。**
   - 选择：根据预算是否覆盖字段目录、规则、业务术语、使用示例、能力清单和目标 scope 命中情况给出风险等级。
   - 原因：AI 可以据此决定继续、收窄 query、提高预算或停止等待人工确认。
   - 备选：返回单一 pass/fail。放弃原因是低预算仍可能适合简单任务，二元判断过粗。

5. **CLI 和前端只消费同一后端 planner。**
   - 选择：CLI 不在本地重复估算，只包装后端 API；前端同样调用后端 API。
   - 原因：避免三套估算规则漂移；测试可集中覆盖服务层和 CLI 参数转发。

## Risks / Trade-offs

- **估算不等于真实模型 token** → 输出 `estimationMethod`、`confidence` 和 fallbackSteps，明确这是保守估算。
- **低预算建议误导 AI 继续执行复杂任务** → `qualityRisk=HIGH` 时 recommendedNextActions 要提示提高预算或收窄任务。
- **planner 读取过多内容影响性能** → 第一版复用现有字段/规则/示例查询和轻量统计，不生成 zip、不序列化完整文件给前端。
- **前端参数和导出参数不一致** → planner 返回 `recommendedExportParams`，前端只展示/一键填充，不直接修改用户已输入内容。
- **敏感信息泄漏** → 不返回字段完整内容、规则正文或用户凭据；所有错误和 CLI stderr 沿用既有脱敏。

## Migration Plan

- 新增 API/CLI/前端展示均为向后兼容；现有 `export-context`、field catalog preview 和 package download 不改变默认语义。
- CLI 新命令失败返回 2，服务端不可达时输出既有 DataSpecError，不影响现有命令。
- 若后续需要模型 tokenizer、向量检索或自动导出包，由新的 SDD change 处理。
