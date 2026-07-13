## Why

DataSpec 已声明字段历史名可解析、Evidence claim 会被确定性验证，但当前实现只读取字段现值，并把所有 `dataspec://evidence/...` 一律标成“无法验证”。这会让 AI 在字段改名后失去稳定引用，也会让伪造、跨项目和真实证据得到相同结果，削弱后续 AI/PR 评审的可信度。

## What Changes

- 从现有字段变更日志快照派生历史名称和历史别名，接入 stable reference resolve、字段检索与字段推荐；不新增历史表、不迁移或回填无法可靠推导的数据。
- 为 Evidence Package 的持久化来源生成项目级 canonical evidence ref，并新增内部 resolver，区分 `VERIFIED`、`MISSING`、`CROSS_PROJECT` 和 `UNVERIFIABLE`。
- 让 AI output post-check 对真实 Evidence claim 放行，对缺失、跨项目和不可验证 claim 返回稳定、脱敏的诊断；保持现有响应字段兼容。
- 修正 SQL PR review 主规格中“只发 summary”与“可发 inline”的冲突：summary 始终保留，可定位且未重复的问题可追加 inline，无法定位的问题回退 summary。
- 不新增数据库对象、外部依赖、第二种 Evidence Package 格式或新的 PR reviewer。

## Capabilities

### New Capabilities

无。历史别名索引和 Evidence resolver 是现有能力的内部实现边界，不新增独立公共 API。

### Modified Capabilities

- `stable-standard-references`：字段引用解析使用可追溯的变更日志历史名，并返回对应变更证据。
- `field-standard-search`：字段检索可由历史名称召回当前字段，输出 matchedAlias 与历史来源证据。
- `field-suggestion`：字段推荐可由历史名称召回当前启用字段，并解释该命中来自历史名称。
- `ai-output-postcheck`：Evidence claim 按项目和持久化来源确定性解析，不再把所有 claim 一律视为不可验证。
- `ai-evidence-package`：持久化证据来源输出可重新解析的 canonical evidence ref；payload 型来源不伪造可验证引用。
- `sql-inline-review-location`：PR review 保持 summary，同时允许符合条件的 inline 评论并记录 fallback。

## Impact

- 后端：字段变更日志查询、历史别名派生、标准引用解析、字段检索/推荐、Evidence Package 和 AI output post-check。
- 外部契约：Evidence Package 增加可选 evidence ref；post-check 只增加新的稳定诊断码和已验证 evidence link，不删除或重命名现有字段。
- CLI/MCP/前端：沿用现有 post-check 和 Evidence Package 响应；更新 OpenAPI 生成类型与契约 fixture。
- 数据与安全：所有查询保持 project-scoped；跨项目来源不暴露目标对象信息；不保存 raw AI output，不输出变更快照原文或秘密。
