## Why

用户和 AI 常从“我要建一个订单表/会员表/支付流水表”这类自然语言开始；系统应先把需求拆成可选标准字段、缺失候选和歧义点，再生成 DDL 或 Prompt。

## What Changes

- 新增自然语言建表需求草案 API，把业务描述、目标表名和可选分组拆成结构化建议。
- 第一版输出可采用的标准字段、缺失候选、歧义点、推荐模板、下一步动作和可复制 Prompt。
- 新增前端入口，便于个人/AI 从一句需求开始进入字段标准、DDL 预览或标准候选 Inbox。

## Capabilities

### New Capabilities
- `requirement-draft`: 自然语言需求到标准候选草案。

### Modified Capabilities
- 无。

## Impact

- 已有基础：字段推荐/检索、业务术语表、标准候选 Inbox、DDL 生成、AI Prompt、表模板和 AI Context。
- 缺口：缺少面向自然语言需求的结构化入口，当前需要 AI 自己拼接检索、推荐、模板和 DDL 生成，容易漏字段或误选泛化字段。
- 落地产物：新增需求草案 API 和前端入口，输入业务描述、目标表名和可选分组，输出 matchedFields、missingCandidates、ambiguousTerms、recommendedTemplate、nextActions 和可复制 Prompt。
- 验收标准：输入一段建表需求后，系统能列出建议采用的标准字段、需要新增的候选字段和不确定问题；结果可继续进入 DDL 预览或标准候选 Inbox。
- 边界：第一版不调用外部 LLM，不自动写入字段库，不承诺完整领域建模，只做确定性检索和模板化草案。
