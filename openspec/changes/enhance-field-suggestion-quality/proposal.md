## Why

字段推荐已经能按字段名、显示名、注释、别名、分类和标签做确定性匹配，但对中文同义词、拼音缩写、泛化业务词、敏感字段和相近语义的区分还不够稳定。DataSpec 优先给 AI 建表使用，推荐质量会直接影响 AI 是否复用个人标准字段，而不是生成新的野字段。

## What Changes

- 增强字段推荐评分模型，加入轻量内置语义词库，覆盖常见中文同义词、英文别名和拼音缩写。
- 对泛化词做降权，避免“用户”“订单”“金额”这类宽泛描述压过更精确的字段。
- 对敏感字段命中输出可解释提示，帮助 AI 和前端知道该字段需要按敏感数据处理。
- 增强 fallback 字段名生成，优先生成更标准的 `user_id`、`mobile_no`、`amount_cent` 等候选名。
- 保持现有 `/api/fields/suggest`、CLI `suggest-field` 和 MCP `suggest_fields` 返回结构兼容，不新增外部 LLM 或向量数据库。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `field-suggestion`: 字段推荐需要更稳定地区分同义词、拼音缩写、泛化词和敏感字段，并输出可解释命中原因。

## Impact

- 后端字段推荐评分逻辑和单元测试。
- `FieldSuggestion.matchReason` 的解释文本更丰富，但字段结构保持兼容。
- CLI/MCP 继续透传后端 JSON，不需要新增命令参数。
- README/TODO/OpenSpec 记录 P5-6 第一版交付边界。
